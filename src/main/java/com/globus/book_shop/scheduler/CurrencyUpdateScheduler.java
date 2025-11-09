package com.globus.book_shop.scheduler;

import com.globus.book_shop.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrencyUpdateScheduler {

    private final CurrencyService currencyService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void fakeCurrencyUpdate() {
        log.info("Fake currency update scheduler executed at {}", LocalDateTime.now());
        log.info("Currency rates have been updated (fake update)");
    }

    @Scheduled(cron = "0 5 0 * * ?")
    public void realCurrencyUpdate() {
        log.info("Real currency update scheduler started at {}", LocalDateTime.now());
        try {
            BigDecimal usdRate = currencyService.getUsdRate();
            log.info("Currency rates updated successfully. Current USD rate: {}", usdRate);
        } catch (Exception e) {
            log.error("Failed to update currency rates: {}", e.getMessage(), e);
        }
    }
}