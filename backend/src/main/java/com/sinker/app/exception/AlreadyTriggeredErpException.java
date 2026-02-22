package com.sinker.app.exception;

public class AlreadyTriggeredErpException extends RuntimeException {
    public AlreadyTriggeredErpException(String message) {
        super(message);
    }
}
