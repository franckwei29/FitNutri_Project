package com.fitnutri.backend.speech;

import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.common.RateLimitedException;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Actual bounded decode through stdin/stdout only. No shell, file/URL input or temporary media. */
@Component
public final class AudioValidator {
    private static final int MAX_INPUT=8*1024*1024,SAMPLE_RATE=16000,MAX_PCM=60*SAMPLE_RATE*2;
    private static final Semaphore DECODERS=new Semaphore(2);
    private final String executable;private final long timeoutMillis;private final boolean ready;
    public AudioValidator(@Value("${app.audio.ffmpeg:ffmpeg}")String executable,@Value("${app.audio.decode-timeout-ms:10000}")long timeoutMillis){
        if(executable==null||executable.isBlank()||executable.length()>1024||executable.chars().anyMatch(Character::isISOControl)||timeoutMillis<100||timeoutMillis>30000)throw new IllegalArgumentException("Invalid audio decoder configuration.");
        this.executable=executable;this.timeoutMillis=timeoutMillis;this.ready=probe();
    }
    private boolean probe(){Process process=null;try{process=new ProcessBuilder(executable,"-version").redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();return process.waitFor(2,TimeUnit.SECONDS)&&process.exitValue()==0;}catch(IOException ignored){return false;}catch(InterruptedException ignored){Thread.currentThread().interrupt();return false;}finally{if(process!=null){if(process.isAlive())process.destroyForcibly();close(process.getOutputStream());close(process.getInputStream());close(process.getErrorStream());}}}
    public boolean ready(){return ready;}
    public PreparedAudio read(MultipartFile file){
        if(!ready)throw unavailable();
        if(file==null||file.isEmpty()||file.getSize()>MAX_INPUT)throw invalid("Provide nonempty WebM or MP4 audio no larger than 8 MiB.");
        String mime=Objects.toString(file.getContentType(),"").split(";",2)[0].strip().toLowerCase(Locale.ROOT);
        byte[] bytes;try{bytes=file.getBytes();}catch(IOException ignored){throw invalid("The audio could not be read.");}
        if(bytes.length==0||bytes.length>MAX_INPUT||!signature(bytes,mime))throw invalid("The audio content does not match its declared WebM or MP4 type.");
        if(!DECODERS.tryAcquire())throw new RateLimitedException(2);
        try{return decode(bytes,mime);}finally{DECODERS.release();}
    }
    private PreparedAudio decode(byte[] bytes,String mime){
        String demuxer=mime.equals("audio/webm")?"matroska":"mov";
        Process process;try{process=new ProcessBuilder(executable,"-hide_banner","-loglevel","error","-nostdin","-max_alloc","33554432","-xerror","-threads","1","-filter_threads","1","-protocol_whitelist","pipe","-f",demuxer,"-i","pipe:0","-map","0:a:0","-vn","-sn","-dn","-ac","1","-ar",String.valueOf(SAMPLE_RATE),"-t","61","-f","s16le","pipe:1").redirectError(ProcessBuilder.Redirect.DISCARD).start();}catch(IOException ignored){throw unavailable();}
        try(var executor=Executors.newVirtualThreadPerTaskExecutor()){
            try{
                Future<?> input=executor.submit(()->{try(var stream=process.getOutputStream()){stream.write(bytes);}catch(IOException ignored){/* Decode/exit status below determines failure; no raw diagnostic. */}});
                Future<byte[]> output=executor.submit(()->{try(var stream=process.getInputStream();var pcm=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=stream.read(buffer))!=-1){if(n>MAX_PCM-pcm.size()){process.destroyForcibly();throw invalid("Record audio no longer than 60 seconds.");}pcm.write(buffer,0,n);}return pcm.toByteArray();}});
                if(!process.waitFor(timeoutMillis,TimeUnit.MILLISECONDS))throw invalid("Audio decoding timed out. Try a shorter recording.");
                byte[] pcm=output.get(1,TimeUnit.SECONDS);input.get(1,TimeUnit.SECONDS);
                if(process.exitValue()!=0||pcm.length<320||pcm.length%2!=0)throw invalid("This file has no decodable audio. Record WebM or MP4 audio again.");
                var wav=ByteBuffer.allocate(44+pcm.length).order(ByteOrder.LITTLE_ENDIAN);wav.put(new byte[]{'R','I','F','F'}).putInt(36+pcm.length).put(new byte[]{'W','A','V','E','f','m','t',' '}).putInt(16).putShort((short)1).putShort((short)1).putInt(SAMPLE_RATE).putInt(SAMPLE_RATE*2).putShort((short)2).putShort((short)16).put(new byte[]{'d','a','t','a'}).putInt(pcm.length).put(pcm);
                return new PreparedAudio(wav.array(),pcm.length*1000L/(SAMPLE_RATE*2));
            }catch(ExecutionException e){if(e.getCause() instanceof BusinessException b)throw b;throw invalid("This audio could not be decoded.");}
            catch(TimeoutException ignored){throw invalid("Audio decoding timed out. Try a shorter recording.");}
            catch(InterruptedException ignored){Thread.currentThread().interrupt();throw invalid("Audio processing was interrupted. Try again.");}
            finally{if(process.isAlive())process.destroyForcibly();close(process.getOutputStream());close(process.getInputStream());}
        }
    }
    private static void close(Closeable stream){try{stream.close();}catch(IOException ignored){}}
    private static boolean signature(byte[] b,String mime){return switch(mime){case "audio/webm"->b.length>4&&(b[0]&255)==0x1a&&(b[1]&255)==0x45&&(b[2]&255)==0xdf&&(b[3]&255)==0xa3;case "audio/mp4"->b.length>=12&&b[4]=='f'&&b[5]=='t'&&b[6]=='y'&&b[7]=='p';default->false;};}
    private static BusinessException invalid(String message){return BusinessException.invalid("audio",message);}
    private static BusinessException unavailable(){return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"FEATURE_NOT_CONFIGURED","Audio processing is currently unavailable.");}
    public static final class PreparedAudio {
        private final byte[] bytes;private final long durationMillis;
        private PreparedAudio(byte[] bytes,long durationMillis){this.bytes=bytes;this.durationMillis=durationMillis;}
        public byte[] bytes(){return bytes.clone();}public String mimeType(){return "audio/wav";}public long durationMillis(){return durationMillis;}
    }
}
