package com.globus.book_shop.mapper;

import com.globus.book_shop.dto.BookRequest;
import com.globus.book_shop.dto.BookResponse;
import com.globus.book_shop.dto.UpdatePriceRequest;
import com.globus.book_shop.entity.Book;
import com.globus.book_shop.entity.BookPrice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface BookMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bookPrice", ignore = true)
    Book toEntity(BookRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "bookPrice", ignore = true)
    void updateEntityFromRequest(BookRequest request, @MappingTarget Book book);

    @Mapping(target = "id", source = "book.id")
    @Mapping(target = "title", source = "book.title")
    @Mapping(target = "author", source = "book.author")
    @Mapping(target = "priceUsd", source = "bookPrice.priceUsd")
    @Mapping(target = "priceRub", source = "priceRub")
    BookResponse toResponse(Book book, BookPrice bookPrice, BigDecimal priceRub);

    default BookResponse toResponse(Book book, BigDecimal priceRub) {
        return toResponse(book, book.getBookPrice(), priceRub);
    }

    @Mapping(target = "priceUsd", source = "request.priceUsd")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "book", ignore = true)
    @Mapping(target = "lastPriceUpdate", ignore = true)
    void updatePriceFromRequest(UpdatePriceRequest request, @MappingTarget BookPrice bookPrice);
}

