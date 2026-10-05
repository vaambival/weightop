package com.weightop.exception;

public class InvalidSortException extends RuntimeException {

    public InvalidSortException(String value, String reason) {
        super("Invalid value '" + value + "' for parameter 'sort': " + reason);
    }
}
