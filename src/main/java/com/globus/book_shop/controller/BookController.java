package com.globus.book_shop.controller;

import com.globus.book_shop.dto.BookRequest;
import com.globus.book_shop.dto.BookResponse;
import com.globus.book_shop.dto.PagedBookResponse;
import com.globus.book_shop.dto.UpdatePriceRequest;
import com.globus.book_shop.service.BookService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
@Validated
public class BookController {

    private final BookService bookService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public PagedBookResponse getAllBooks(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "Page must be >= 0") int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Size must be >= 1")
            @Max(value = 100, message = "Size must be <= 100") int size) {
        log.info("GET request to /api/books - get all books, page: {}, size: {}", page, size);
        return bookService.findAll(page, size);
    }

    @GetMapping("/{id}")
    public BookResponse getBookById(
            @PathVariable @Min(value = 1, message = "Book ID must be >= 1") Long id) {
        log.debug("GET /api/books/{}", id);
        BookResponse book = bookService.findById(id);
        log.info("Retrieved book with id: {}", id);
        return book;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponse createBook(@Valid @RequestBody BookRequest request) {
        log.debug("POST /api/books - title: {}, author: {}, priceUsd: {}", 
                request.getTitle(), request.getAuthor(), request.getPriceUsd());
        BookResponse book = bookService.create(request);
        log.info("Created book with id: {}, title: {}", book.getId(), book.getTitle());
        return book;
    }

    @PutMapping("/{id}")
    public BookResponse updateBook(
            @PathVariable @Min(value = 1, message = "Book ID must be >= 1") Long id,
            @Valid @RequestBody BookRequest request) {
        log.debug("PUT /api/books/{} - title: {}, author: {}, priceUsd: {}", 
                id, request.getTitle(), request.getAuthor(), request.getPriceUsd());
        BookResponse book = bookService.update(id, request);
        log.info("Updated book with id: {}", id);
        return book;
    }

    @PatchMapping("/{id}/price")
    public BookResponse updatePrice(
            @PathVariable @Min(value = 1, message = "Book ID must be >= 1") Long id,
            @Valid @RequestBody UpdatePriceRequest request) {
        log.debug("PATCH /api/books/{}/price - priceUsd: {}", id, request.getPriceUsd());
        BookResponse book = bookService.updatePrice(id, request);
        log.info("Updated price for book with id: {}, new price: {}", id, request.getPriceUsd());
        return book;
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable @Min(value = 1, message = "Book ID must be >= 1") Long id) {
        log.debug("DELETE /api/books/{}", id);
        bookService.delete(id);
        log.info("Deleted book with id: {}", id);
    }
}