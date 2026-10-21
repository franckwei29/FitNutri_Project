package com.fitnutri.backend.food;

import com.fitnutri.backend.common.BusinessException;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Locale;
import javax.imageio.*;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Validates actual decoded pixels, then reencodes without input metadata or original filename. */
@Component
public final class ImageValidator {
    private static final int MAX_BYTES=8*1024*1024,MAX_DIMENSION=4096;
    public PreparedImage read(MultipartFile file){
        if(file==null||file.isEmpty()||file.getSize()>MAX_BYTES)throw invalid("Provide a still JPEG, PNG or WebP image no larger than 8 MiB.");
        String mime=file.getContentType();if(mime==null)throw invalid("Choose a JPEG, PNG or WebP image.");mime=mime.toLowerCase(Locale.ROOT);
        byte[] bytes;try{bytes=file.getBytes();}catch(IOException ignored){throw invalid("The image could not be read.");}
        if(bytes.length==0||bytes.length>MAX_BYTES||!signature(bytes,mime))throw invalid("The image content does not match its declared JPEG, PNG or WebP type.");
        try(var stream=new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))){
            var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw invalid("This image cannot be decoded. Choose another JPEG, PNG or WebP file.");
            ImageReader reader=readers.next();
            try{
                reader.setInput(stream,true,true); // ignore metadata; never extract EXIF/location or preserve it.
                int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width<1||height<1||width>MAX_DIMENSION||height>MAX_DIMENSION||(long)width*height>16_777_216L)throw invalid("Use an image no larger than 4,096 pixels on either side.");
                BufferedImage decoded=reader.read(0);if(decoded==null||decoded.getWidth()!=width||decoded.getHeight()!=height)throw invalid("This image could not be decoded.");
                var opaque=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);var graphics=opaque.createGraphics();try{graphics.setColor(Color.WHITE);graphics.fillRect(0,0,width,height);graphics.drawImage(decoded,0,0,null);}finally{graphics.dispose();decoded.flush();}
                try(var encoded=new LimitedOutput();var output=new MemoryCacheImageOutputStream(encoded)){
                    if(!ImageIO.write(opaque,"jpeg",output))throw invalid("The image could not be prepared.");
                    output.flush();
                    return new PreparedImage(encoded.toByteArray());
                }finally{opaque.flush();}
            }finally{reader.dispose();}
        }catch(BusinessException e){throw e;}catch(IOException|RuntimeException ignored){throw invalid("This image could not be decoded or prepared. Try a smaller JPEG, PNG or WebP image.");}
    }
    private static boolean signature(byte[] b,String mime){return switch(mime){
        case "image/jpeg"->b.length>=3&&(b[0]&255)==255&&(b[1]&255)==216&&(b[2]&255)==255;
        case "image/png"->b.length>=8&&(b[0]&255)==137&&b[1]==80&&b[2]==78&&b[3]==71&&b[4]==13&&b[5]==10&&b[6]==26&&b[7]==10;
        case "image/webp"->b.length>=12&&b[0]=='R'&&b[1]=='I'&&b[2]=='F'&&b[3]=='F'&&b[8]=='W'&&b[9]=='E'&&b[10]=='B'&&b[11]=='P';
        default->false;
    };}
    private static BusinessException invalid(String message){return BusinessException.invalid("image",message);}
    public static final class PreparedImage {
        private final byte[] bytes;
        private PreparedImage(byte[] bytes){this.bytes=bytes;}
        public byte[] bytes(){return bytes.clone();}
        public String mimeType(){return "image/jpeg";}
    }
    private static final class LimitedOutput extends ByteArrayOutputStream {
        @Override public synchronized void write(byte[] b,int off,int len){if(len>MAX_BYTES-count)throw invalid("The prepared image is too large. Choose a smaller image.");super.write(b,off,len);}
        @Override public synchronized void write(int b){if(count>=MAX_BYTES)throw invalid("The prepared image is too large. Choose a smaller image.");super.write(b);}
    }
}
