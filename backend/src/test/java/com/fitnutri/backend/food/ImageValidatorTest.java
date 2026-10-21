package com.fitnutri.backend.food;

import com.fitnutri.backend.common.BusinessException;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;

class ImageValidatorTest {
    private final ImageValidator validator=new ImageValidator();
    private byte[] image(String format,int width,int height) throws Exception {var bytes=new ByteArrayOutputStream();assertTrue(ImageIO.write(new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB),format,bytes));return bytes.toByteArray();}
    @ParameterizedTest @CsvSource({"png,image/png","jpeg,image/jpeg"})
    void actualImageIsDecodedAndReencodedToMetadataFreeJpeg(String format,String mime) throws Exception {
        byte[] original=image(format,3,2);
        var result=validator.read(new MockMultipartFile("image","untrusted-original-name",mime,original));
        assertEquals("image/jpeg",result.mimeType());var decoded=ImageIO.read(new ByteArrayInputStream(result.bytes()));assertEquals(3,decoded.getWidth());assertEquals(2,decoded.getHeight());assertEquals((byte)0xff,result.bytes()[0]);assertEquals((byte)0xd8,result.bytes()[1]);
    }
    @Test void actualWebpFixtureUsesInstalledDecoderAndBecomesARealJpeg() throws Exception {
        byte[] bytes;try(var stream=getClass().getResourceAsStream("/fixtures/ai/synthetic-2x2.webp")){assertNotNull(stream);bytes=stream.readAllBytes();}
        var result=validator.read(new MockMultipartFile("image","synthetic.webp","image/webp",bytes));var decoded=ImageIO.read(new ByteArrayInputStream(result.bytes()));assertEquals(2,decoded.getWidth());assertEquals(2,decoded.getHeight());assertEquals("image/jpeg",result.mimeType());
    }
    @Test void mimeSignatureMismatchTruncationEmptyAndUnsupportedBytesNeverPassAsImages() throws Exception {
        for(var file:new MockMultipartFile[]{new MockMultipartFile("image","x.jpg","image/jpeg",image("png",1,1)),new MockMultipartFile("image","x.png","image/png","not an image".getBytes()),new MockMultipartFile("image","x.webp","image/webp",new byte[]{'R','I','F','F'}),new MockMultipartFile("image","x.jpg","image/jpeg",new byte[0]),new MockMultipartFile("image","x.gif","image/gif",image("png",1,1)),new MockMultipartFile("image","x",null,image("png",1,1))}){var error=assertThrows(BusinessException.class,()->validator.read(file));assertEquals(400,error.status().value());assertFalse(error.getMessage().contains("not an image"));}
    }
    @Test void declaredOversizeAndDecodedDimensionsAreRejectedBeforeUnboundedAllocation() throws Exception {
        var huge=new MockMultipartFile("image","x.png","image/png",new byte[8*1024*1024+1]);assertThrows(BusinessException.class,()->validator.read(huge));
        var wide=new MockMultipartFile("image","wide.png","image/png",image("png",4097,1));assertThrows(BusinessException.class,()->validator.read(wide));
    }
    @Test void originalExifLikeMetadataIsNotSentToProvider() throws Exception {
        byte[] jpeg=image("jpeg",2,2),metadata="Exif\0\0SYNTHETIC_GPS_LOCATION_ONLY".getBytes(StandardCharsets.US_ASCII);var bytes=new ByteArrayOutputStream();bytes.write(jpeg,0,2);bytes.write(0xff);bytes.write(0xe1);bytes.write((metadata.length+2)>>8);bytes.write((metadata.length+2)&255);bytes.write(metadata);bytes.write(jpeg,2,jpeg.length-2);
        var result=validator.read(new MockMultipartFile("image","photo.jpg","image/jpeg",bytes.toByteArray()));assertFalse(new String(result.bytes(),StandardCharsets.ISO_8859_1).contains("SYNTHETIC_GPS_LOCATION_ONLY"));assertNotNull(ImageIO.read(new ByteArrayInputStream(result.bytes())));
    }
}
