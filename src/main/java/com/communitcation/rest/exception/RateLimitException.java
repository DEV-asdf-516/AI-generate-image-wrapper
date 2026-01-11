package com.communitcation.rest.exception;

import lombok.Getter;

@Getter
public class RateLimitException extends RuntimeException {

    private final long remainingSeconds;

    public RateLimitException(long remainingSeconds) {
        super(remainingSeconds + "초 후에 다시 시도해주세요");
        this.remainingSeconds = remainingSeconds;
    }
}








