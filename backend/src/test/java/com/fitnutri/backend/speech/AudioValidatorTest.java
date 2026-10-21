package com.fitnutri.backend.speech;

import com.fitnutri.backend.common.BusinessException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.attribute.PosixFilePermissions;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;

class AudioValidatorTest {
    final AudioValidator validator=new AudioValidator(System.getenv().getOrDefault("FFMPEG_BIN","ffmpeg"),10000);
    byte[] fixture(String name) throws Exception {try(var input=getClass().getResourceAsStream("/fixtures/ai/"+name)){assertNotNull(input);return input.readAllBytes();}}
    MockMultipartFile file(String mime,byte[] bytes){return new MockMultipartFile("audio","private-original-name",mime,bytes);}
    @Test void decodesActualWebmAndFragmentedMp4ToMetadataFreeInMemoryWav() throws Exception {
        assertTrue(validator.ready(),"FFmpeg must be installed for actual audio validation tests; do not silently skip");
        for(String name:new String[]{"synthetic-tone.webm","synthetic-tone.mp4"}){
            var result=validator.read(file(name.endsWith("webm")?"audio/webm;codecs=opus":"audio/mp4",fixture(name)));
            byte[] wav=result.bytes();assertEquals("audio/wav",result.mimeType());assertEquals("RIFF",new String(wav,0,4,java.nio.charset.StandardCharsets.US_ASCII));assertEquals("WAVE",new String(wav,8,4,java.nio.charset.StandardCharsets.US_ASCII));assertEquals(16000,ByteBuffer.wrap(wav,24,4).order(ByteOrder.LITTLE_ENDIAN).getInt());assertTrue(result.durationMillis()>=900&&result.durationMillis()<=1100);assertTrue(wav.length>28000&&wav.length<36000);assertFalse(new String(wav,java.nio.charset.StandardCharsets.ISO_8859_1).contains("SYNTHETIC_PRIVATE_METADATA"));wav[0]=0;assertEquals('R',result.bytes()[0]);
        }
    }
    @Test void rejectsEmptyOversizeUnsupportedAndCounterfeitMediaBeforeDecoding() throws Exception {
        for(var input:new MockMultipartFile[]{file("audio/webm",new byte[0]),file("audio/webm",new byte[8*1024*1024+1]),file("audio/wav",fixture("synthetic-tone.mp4")),file("audio/mp4",fixture("synthetic-tone.webm")),file("audio/webm",new byte[]{0x1a,0x45,(byte)0xdf,(byte)0xa3}),file("audio/webm",java.util.Arrays.copyOf(fixture("synthetic-tone.webm"),64))})assertThrows(BusinessException.class,()->validator.read(input));
    }
    @Test void rejectsRealDecodedAudioLongerThanSixtySeconds() throws Exception {var error=assertThrows(BusinessException.class,()->validator.read(file("audio/webm",fixture("synthetic-long-tone.webm"))));assertEquals("VALIDATION_ERROR",error.code());}
    @Test void unavailableDecoderIsNotReadyAndNeverPretendsSuccess(){var missing=new AudioValidator("/nonexistent/fitnutri-owned-test-no-ffmpeg",100);assertFalse(missing.ready());assertEquals("FEATURE_NOT_CONFIGURED",assertThrows(BusinessException.class,()->missing.read(file("audio/webm",new byte[]{1}))).code());}
    @Test void decoderTimeoutTerminatesItsOwnProcess(@TempDir Path directory) throws Exception {
        Path pid=directory.resolve("decoder.pid"),script=directory.resolve("controlled-decoder.sh");
        Files.writeString(script,"#!/bin/sh\nif [ \"$1\" = \"-version\" ]; then exit 0; fi\necho $$ > '"+pid+"'\nexec /bin/sleep 10\n");Files.setPosixFilePermissions(script,PosixFilePermissions.fromString("rwx------"));var stalled=new AudioValidator(script.toString(),200);assertTrue(stalled.ready());
        assertEquals("VALIDATION_ERROR",assertThrows(BusinessException.class,()->stalled.read(file("audio/webm",fixture("synthetic-tone.webm")))).code());
        long processId=Long.parseLong(Files.readString(pid).strip()),deadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(1);while(ProcessHandle.of(processId).map(ProcessHandle::isAlive).orElse(false)&&System.nanoTime()<deadline)Thread.sleep(10);assertFalse(ProcessHandle.of(processId).map(ProcessHandle::isAlive).orElse(false));
    }
}
