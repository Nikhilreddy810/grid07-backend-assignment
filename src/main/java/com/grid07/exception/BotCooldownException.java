package com.grid07.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class BotCooldownException extends RuntimeException {
    public BotCooldownException(String message) {
        super(message);
    }
}
