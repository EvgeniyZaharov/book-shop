package com.globus.book_shop.service;

import java.math.BigDecimal;

public interface CurrencyService {
    BigDecimal getUsdRate();
    BigDecimal convertUsdToRub(BigDecimal usdAmount);
}