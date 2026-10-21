package com.fitnutri.backend.ai;

import com.fitnutri.backend.common.BusinessException;
import com.fitnutri.backend.common.RateLimitedException;
import org.springframework.http.HttpStatus;

/** Never retain provider bodies, exception causes, credentials or raw headers in public errors. */
public final class AiExceptionMapper {
    private AiExceptionMapper(){}
    public static BusinessException upstream(int status,String retryAfter){
        if(status==429){int seconds=30;if(retryAfter!=null&&retryAfter.matches("[0-9]{1,3}"))seconds=Math.max(1,Math.min(Integer.parseInt(retryAfter),300));return new RateLimitedException(seconds);}
        if(status==401||status==403)return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"FEATURE_NOT_CONFIGURED","This AI service is unavailable with the current provider configuration.");
        return badResponse();
    }
    public static BusinessException badResponse(){return new BusinessException(HttpStatus.BAD_GATEWAY,"PROVIDER_BAD_RESPONSE","The AI service did not return a usable result. Try again later.");}
    public static BusinessException timeout(){return new BusinessException(HttpStatus.GATEWAY_TIMEOUT,"PROVIDER_TIMEOUT","The AI service timed out. Try again later.");}
    public static BusinessException unavailable(){return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE,"FEATURE_NOT_CONFIGURED","This AI feature is not configured or enabled.");}
    public static BusinessException invalidRequest(){return new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,"AI_REQUEST_BOUNDARY","The AI request could not be prepared.");}
}
