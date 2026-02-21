package com.sinker.app.exception;

import java.util.List;

public class ExcelParseException extends RuntimeException {

    private final List<String> errors;

    public ExcelParseException(String message) {
        super(message);
        this.errors = List.of(message);
    }

    public ExcelParseException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
