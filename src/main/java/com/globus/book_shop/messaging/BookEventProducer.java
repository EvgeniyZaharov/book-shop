package com.globus.book_shop.messaging;

import com.globus.book_shop.dto.BookEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookEventProducer {

    private static final String TOPIC = "book-events";

    private final KafkaTemplate<String, BookEvent> kafkaTemplate;

    public void sendBookEvent(BookEvent event) {
        String key = String.valueOf(event.getBookId());
        
        CompletableFuture<SendResult<String, BookEvent>> future = kafkaTemplate.send(TOPIC, key, event);
        
        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Book event sent successfully: bookId={}, eventType={}, offset={}", 
                        event.getBookId(), event.getEventType(), result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send book event: bookId={}, eventType={}", 
                        event.getBookId(), event.getEventType(), ex);
            }
        });
    }
}

