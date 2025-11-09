package com.globus.book_shop.exception;

public class CurrencyServiceException extends RuntimeException {
    public CurrencyServiceException(String message) {
        super(message);
    }

    public CurrencyServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}