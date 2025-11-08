package com.globus.book_shop.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class BookResponse {
    Long id;
    String title;
    String author;
    BigDecimal priceUsd;
    BigDecimal priceRub;
}

