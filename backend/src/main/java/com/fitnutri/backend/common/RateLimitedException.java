package com.fitnutri.backend.common;

import org.springframework.http.HttpStatus;

/** Public retry metadata only; never retain an upstream header or response body. */
public final class RateLimitedException extends BusinessException {
    private final int retryAfterSeconds;
    public RateLimitedException(int retryAfterSeconds){super(HttpStatus.TOO_MANY_REQUESTS,"RATE_LIMITED","The AI service is busy. Wait before trying again.");this.retryAfterSeconds=Math.max(1,Math.min(retryAfterSeconds,300));}
    public int retryAfterSeconds(){return retryAfterSeconds;}
}
