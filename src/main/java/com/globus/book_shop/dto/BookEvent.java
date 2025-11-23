package com.globus.book_shop.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
@Jacksonized
public class BookEvent {
    Long bookId;
    String title;
    String author;
    BigDecimal priceUsd;
    EventType eventType;
    LocalDateTime timestamp;

    public enum EventType {
        CREATED, UPDATED, PRICE_UPDATED, DELETED
    }
}