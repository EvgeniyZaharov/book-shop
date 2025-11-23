package com.globus.book_shop.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PagedBookResponse {
    List<BookResponse> books;
    int page;
    int totalPages;
    long totalElements;
    int size;
}