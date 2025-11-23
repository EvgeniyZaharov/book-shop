package com.globus.book_shop.service.impl;

import com.globus.book_shop.dto.BookEvent;
import com.globus.book_shop.dto.BookRequest;
import com.globus.book_shop.dto.BookResponse;
import com.globus.book_shop.dto.PagedBookResponse;
import com.globus.book_shop.dto.UpdatePriceRequest;
import com.globus.book_shop.entity.Book;
import com.globus.book_shop.entity.BookPrice;
import com.globus.book_shop.exception.BookNotFoundException;
import com.globus.book_shop.exception.BookPriceNotFoundException;
import com.globus.book_shop.mapper.BookMapper;
import com.globus.book_shop.messaging.BookEventProducer;
import com.globus.book_shop.repository.BookPriceRepository;
import com.globus.book_shop.repository.BookRepository;
import com.globus.book_shop.service.BookService;
import com.globus.book_shop.service.CurrencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final BookPriceRepository bookPriceRepository;
    private final BookMapper bookMapper;
    private final CurrencyService currencyService;
    private final BookEventProducer bookEventProducer;

    @Override
    @Transactional(readOnly = true)
    public PagedBookResponse findAll(int page, int size) {
        log.debug("Finding all books - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Book> booksPage = bookRepository.findAllWithPrices(pageable);
        log.debug("Found {} books from database (page {} of {})", 
                booksPage.getNumberOfElements(), booksPage.getNumber(), booksPage.getTotalPages());
        
        Page<BookResponse> responsePage = booksPage.map(this::toResponse);
        
        return PagedBookResponse.builder()
                .books(responsePage.getContent())
                .page(responsePage.getNumber())
                .totalPages(responsePage.getTotalPages())
                .totalElements(responsePage.getTotalElements())
                .size(responsePage.getSize())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BookResponse findById(Long id) {
        log.debug("Finding book by id: {}", id);
        Book book = findBookWithPriceOrThrow(id, "find");
        log.debug("Book found: id={}, title={}", book.getId(), book.getTitle());
        return toResponse(book);
    }

    @Override
    public BookResponse create(BookRequest request) {
        log.debug("Creating new book - title: {}, author: {}, priceUsd: {}", 
                request.getTitle(), request.getAuthor(), request.getPriceUsd());
        Book book = bookMapper.toEntity(request);
        book = bookRepository.save(book);
        log.debug("Book saved with id: {}", book.getId());
        
        BookPrice bookPrice = createBookPrice(book, request.getPriceUsd());
        bookPriceRepository.save(bookPrice);
        log.debug("BookPrice saved for book id: {}", book.getId());
        
        book.setBookPrice(bookPrice);
        
        BookResponse response = toResponse(book);
        
        BookEvent event = BookEvent.builder()
                .bookId(response.getId())
                .title(response.getTitle())
                .author(response.getAuthor())
                .priceUsd(response.getPriceUsd())
                .eventType(BookEvent.EventType.CREATED)
                .timestamp(LocalDateTime.now())
                .build();
        bookEventProducer.sendBookEvent(event);
        
        log.info("Book created successfully - id: {}, title: {}", response.getId(), response.getTitle());
        return response;
    }

    @Override
    public BookResponse update(Long id, BookRequest request) {
        log.debug("Updating book with id: {} - title: {}, author: {}, priceUsd: {}", 
                id, request.getTitle(), request.getAuthor(), request.getPriceUsd());
        Book book = findBookWithPriceOrThrow(id, "update");
        
        bookMapper.updateEntityFromRequest(request, book);
        book = bookRepository.save(book);
        log.debug("Book updated in database");
        
        BookPrice bookPrice = getBookPriceOrThrow(book, id);
        bookPrice.setPriceUsd(request.getPriceUsd());
        updatePriceTimestamp(bookPrice);
        bookPriceRepository.save(bookPrice);
        log.debug("BookPrice updated in database");
        
        BookResponse response = toResponse(book);
        
        BookEvent event = BookEvent.builder()
                .bookId(response.getId())
                .title(response.getTitle())
                .author(response.getAuthor())
                .priceUsd(response.getPriceUsd())
                .eventType(BookEvent.EventType.UPDATED)
                .timestamp(LocalDateTime.now())
                .build();
        bookEventProducer.sendBookEvent(event);
        
        log.info("Book updated successfully - id: {}", id);
        return response;
    }

    @Override
    public BookResponse updatePrice(Long id, UpdatePriceRequest request) {
        log.debug("Updating price for book id: {} - new priceUsd: {}", id, request.getPriceUsd());
        Book book = findBookWithPriceOrThrow(id, "price update");
        
        BookPrice bookPrice = getBookPriceOrThrow(book, id);
        BigDecimal oldPrice = bookPrice.getPriceUsd();
        bookMapper.updatePriceFromRequest(request, bookPrice);
        updatePriceTimestamp(bookPrice);
        bookPriceRepository.save(bookPrice);
        log.debug("BookPrice updated - old price: {}, new price: {}", oldPrice, request.getPriceUsd());
        
        BookResponse response = toResponse(book);
        
        BookEvent event = BookEvent.builder()
                .bookId(response.getId())
                .title(response.getTitle())
                .author(response.getAuthor())
                .priceUsd(response.getPriceUsd())
                .eventType(BookEvent.EventType.PRICE_UPDATED)
                .timestamp(LocalDateTime.now())
                .build();
        bookEventProducer.sendBookEvent(event);
        
        log.info("Price updated successfully for book id: {} - new price: {}", id, request.getPriceUsd());
        return response;
    }

    @Override
    public void delete(Long id) {
        log.debug("Deleting book with id: {}", id);
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Book with id {} not found for deletion", id);
                    return new BookNotFoundException(id);
                });
        
        BookPrice bookPrice = book.getBookPrice();
        BigDecimal priceUsd = bookPrice != null ? bookPrice.getPriceUsd() : null;
        
        bookRepository.deleteById(id);
        
        BookEvent event = BookEvent.builder()
                .bookId(id)
                .title(book.getTitle())
                .author(book.getAuthor())
                .priceUsd(priceUsd)
                .eventType(BookEvent.EventType.DELETED)
                .timestamp(LocalDateTime.now())
                .build();
        bookEventProducer.sendBookEvent(event);
        
        log.info("Book deleted successfully - id: {}", id);
    }

    private BookResponse toResponse(Book book) {
        BookPrice bookPrice = getBookPriceOrThrow(book, book.getId());
        BigDecimal priceRub = currencyService.convertUsdToRub(bookPrice.getPriceUsd());
        return bookMapper.toResponse(book, bookPrice, priceRub);
    }

    private Book findBookWithPriceOrThrow(Long id, String operation) {
        return bookRepository.findByIdWithPrice(id)
                .orElseThrow(() -> {
                    log.warn("Book with id {} not found for {}", id, operation);
                    return new BookNotFoundException(id);
                });
    }

    private BookPrice getBookPriceOrThrow(Book book, Long bookId) {
        BookPrice bookPrice = book.getBookPrice();
        if (bookPrice == null) {
            log.error("BookPrice not found for book id: {}", bookId);
            throw new BookPriceNotFoundException(bookId);
        }
        return bookPrice;
    }

    private BookPrice createBookPrice(Book book, BigDecimal priceUsd) {
        BookPrice bookPrice = new BookPrice();
        bookPrice.setBook(book);
        bookPrice.setPriceUsd(priceUsd);
        updatePriceTimestamp(bookPrice);
        return bookPrice;
    }

    private void updatePriceTimestamp(BookPrice bookPrice) {
        bookPrice.setLastPriceUpdate(LocalDateTime.now());
    }
}