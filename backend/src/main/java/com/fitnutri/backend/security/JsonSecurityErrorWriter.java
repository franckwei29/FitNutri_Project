package com.fitnutri.backend.security;

import com.fitnutri.backend.common.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class JsonSecurityErrorWriter {
    private final ObjectMapper mapper;
    public JsonSecurityErrorWriter(ObjectMapper mapper) {this.mapper=mapper;}
    public void write(HttpServletRequest request,HttpServletResponse response,int status,String code,String message) throws IOException {
        response.setStatus(status);response.setContentType("application/json");response.setCharacterEncoding("UTF-8");response.setHeader("Cache-Control","no-store");
        mapper.writeValue(response.getOutputStream(),ApiError.create(status,code,message,request,List.of(),status==503));
    }
}
