package com.globus.book_shop.service;

import com.globus.book_shop.dto.BookRequest;
import com.globus.book_shop.dto.BookResponse;
import com.globus.book_shop.dto.PagedBookResponse;
import com.globus.book_shop.dto.UpdatePriceRequest;

public interface BookService {
    PagedBookResponse findAll(int page, int size);
    BookResponse findById(Long id);
    BookResponse create(BookRequest request);
    BookResponse update(Long id, BookRequest request);
    BookResponse updatePrice(Long id, UpdatePriceRequest request);
    void delete(Long id);
}