package com.globus.book_shop.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;

@Value
@Builder
@Jacksonized
public class UpdatePriceRequest {
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    BigDecimal priceUsd;
}

