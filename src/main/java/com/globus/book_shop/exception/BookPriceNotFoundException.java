package com.globus.book_shop.exception;

public class BookPriceNotFoundException extends RuntimeException {
    public BookPriceNotFoundException(String message) {
        super(message);
    }

    public BookPriceNotFoundException(Long bookId) {
        super("Price for book with id " + bookId + " not found");
    }
}