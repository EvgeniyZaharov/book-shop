package com.globus.book_shop.messaging;

import com.globus.book_shop.dto.BookEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class BookEventConsumer {

    private final AtomicLong createdCount = new AtomicLong(0);
    private final AtomicLong updatedCount = new AtomicLong(0);
    private final AtomicLong priceUpdatedCount = new AtomicLong(0);
    private final AtomicLong deletedCount = new AtomicLong(0);

    @KafkaListener(topics = "book-events", groupId = "book-shop-group")
    public void consumeBookEvent(BookEvent event) {
        log.info("📦 Received book event: type={}, bookId={}, title={}, timestamp={}",
                event.getEventType(), event.getBookId(), event.getTitle(), event.getTimestamp());

        switch (event.getEventType()) {
            case CREATED -> handleCreated(event);
            case UPDATED -> handleUpdated(event);
            case PRICE_UPDATED -> handlePriceUpdated(event);
            case DELETED -> handleDeleted(event);
        }

        logStatistics();
    }

    private void handleCreated(BookEvent event) {
        long count = createdCount.incrementAndGet();
        log.info("✅ Book created: id={}, title={}, price={} USD (Total created: {})",
                event.getBookId(), event.getTitle(), event.getPriceUsd(), count);
    }

    private void handleUpdated(BookEvent event) {
        long count = updatedCount.incrementAndGet();
        log.info("🔄 Book updated: id={}, title={}, price={} USD (Total updated: {})",
                event.getBookId(), event.getTitle(), event.getPriceUsd(), count);
    }

    private void handlePriceUpdated(BookEvent event) {
        long count = priceUpdatedCount.incrementAndGet();
        log.info("💰 Price updated: id={}, title={}, newPrice={} USD (Total price updates: {})",
                event.getBookId(), event.getTitle(), event.getPriceUsd(), count);
    }

    private void handleDeleted(BookEvent event) {
        long count = deletedCount.incrementAndGet();
        log.info("🗑️ Book deleted: id={}, title={} (Total deleted: {})",
                event.getBookId(), event.getTitle(), count);
    }

    private void logStatistics() {
        long total = createdCount.get() + updatedCount.get() + priceUpdatedCount.get() + deletedCount.get();
        log.info("📊 Statistics: Total events={} (Created: {}, Updated: {}, PriceUpdated: {}, Deleted: {})",
                total, createdCount.get(), updatedCount.get(), priceUpdatedCount.get(), deletedCount.get());
    }
}

