package com.grid07.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class BotCapExceededException extends RuntimeException {
    public BotCapExceededException(String message) {
        super(message);
    }
}
