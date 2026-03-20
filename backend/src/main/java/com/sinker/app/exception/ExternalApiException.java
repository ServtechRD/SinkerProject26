package com.sinker.app.exception;

/**
 * 呼叫外部整合 API（PDCA、ERP 等）失敗時拋出。
 */
public class ExternalApiException extends RuntimeException {

    public ExternalApiException(String message) {
        super(message);
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
