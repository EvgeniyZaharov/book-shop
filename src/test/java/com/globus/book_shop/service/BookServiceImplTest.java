package com.globus.book_shop.service;

import com.globus.book_shop.dto.BookRequest;
import com.globus.book_shop.dto.BookResponse;
import com.globus.book_shop.dto.PagedBookResponse;
import com.globus.book_shop.dto.UpdatePriceRequest;
import com.globus.book_shop.entity.Book;
import com.globus.book_shop.entity.BookPrice;
import com.globus.book_shop.exception.BookNotFoundException;
import com.globus.book_shop.mapper.BookMapper;
import com.globus.book_shop.repository.BookPriceRepository;
import com.globus.book_shop.repository.BookRepository;
import com.globus.book_shop.service.impl.BookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceImplTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookPriceRepository bookPriceRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private CurrencyService currencyService;

    @InjectMocks
    private BookServiceImpl bookService;

    private Book book;
    private BookPrice bookPrice;
    private BookRequest bookRequest;
    private BookResponse bookResponse;

    @BeforeEach
    void setUp() {
        book = new Book();
        book.setId(1L);
        book.setTitle("Test Book");
        book.setAuthor("Test Author");

        bookPrice = new BookPrice();
        bookPrice.setId(1L);
        bookPrice.setBook(book);
        bookPrice.setPriceUsd(new BigDecimal("29.99"));
        bookPrice.setLastPriceUpdate(LocalDateTime.now());

        book.setBookPrice(bookPrice);

        bookRequest = BookRequest.builder()
                .title("Test Book")
                .author("Test Author")
                .priceUsd(new BigDecimal("29.99"))
                .build();

        bookResponse = BookResponse.builder()
                .id(1L)
                .title("Test Book")
                .author("Test Author")
                .priceUsd(new BigDecimal("29.99"))
                .priceRub(new BigDecimal("2249.25"))
                .build();
    }

    @Test
    void findAll_ShouldReturnPagedBooks() {
        Page<Book> bookPage = new PageImpl<>(List.of(book), PageRequest.of(0, 20), 1);
        when(bookRepository.findAllWithPrices(any(Pageable.class))).thenReturn(bookPage);
        when(currencyService.convertUsdToRub(any(BigDecimal.class))).thenReturn(new BigDecimal("2249.25"));
        when(bookMapper.toResponse(eq(book), eq(bookPrice), any(BigDecimal.class))).thenReturn(bookResponse);

        PagedBookResponse result = bookService.findAll(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getBooks().size());
        assertEquals(bookResponse, result.getBooks().get(0));
        assertEquals(0, result.getPage());
        assertEquals(1, result.getTotalPages());
        assertEquals(20, result.getSize());
        verify(bookRepository).findAllWithPrices(any(Pageable.class));
    }

    @Test
    void findAll_WhenNoBooks_ShouldReturnEmptyPage() {
        Page<Book> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(bookRepository.findAllWithPrices(any(Pageable.class))).thenReturn(emptyPage);

        PagedBookResponse result = bookService.findAll(0, 20);

        assertNotNull(result);
        assertTrue(result.getBooks().isEmpty());
        assertEquals(0, result.getTotalElements());
        verify(bookRepository).findAllWithPrices(any(Pageable.class));
    }

    @Test
    void findById_WhenBookExists_ShouldReturnBook() {
        when(bookRepository.findByIdWithPrice(1L)).thenReturn(Optional.of(book));
        when(currencyService.convertUsdToRub(any(BigDecimal.class))).thenReturn(new BigDecimal("2249.25"));
        when(bookMapper.toResponse(eq(book), eq(bookPrice), any(BigDecimal.class))).thenReturn(bookResponse);

        BookResponse result = bookService.findById(1L);

        assertNotNull(result);
        assertEquals(bookResponse, result);
        verify(bookRepository).findByIdWithPrice(1L);
    }

    @Test
    void findById_WhenBookNotFound_ShouldThrowException() {
        when(bookRepository.findByIdWithPrice(1L)).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class, () -> bookService.findById(1L));
        verify(bookRepository).findByIdWithPrice(1L);
    }

    @Test
    void create_ShouldCreateBookAndBookPrice() {
        Book savedBook = new Book();
        savedBook.setId(1L);
        savedBook.setTitle("Test Book");
        savedBook.setAuthor("Test Author");

        when(bookMapper.toEntity(bookRequest)).thenReturn(savedBook);
        when(bookRepository.save(any(Book.class))).thenReturn(savedBook);
        when(bookPriceRepository.save(any(BookPrice.class))).thenReturn(bookPrice);
        when(currencyService.convertUsdToRub(any(BigDecimal.class))).thenReturn(new BigDecimal("2249.25"));
        when(bookMapper.toResponse(eq(savedBook), eq(bookPrice), any(BigDecimal.class))).thenReturn(bookResponse);

        BookResponse result = bookService.create(bookRequest);

        assertNotNull(result);
        assertEquals(bookResponse, result);
        verify(bookRepository).save(any(Book.class));
        verify(bookPriceRepository).save(argThat(bp -> bp.getPriceUsd().equals(bookRequest.getPriceUsd())));
    }

    @Test
    void update_WhenBookExists_ShouldUpdateBook() {
        when(bookRepository.findByIdWithPrice(1L)).thenReturn(Optional.of(book));
        when(bookRepository.save(any(Book.class))).thenReturn(book);
        when(bookPriceRepository.save(any(BookPrice.class))).thenReturn(bookPrice);
        when(currencyService.convertUsdToRub(any(BigDecimal.class))).thenReturn(new BigDecimal("2249.25"));
        when(bookMapper.toResponse(eq(book), eq(bookPrice), any(BigDecimal.class))).thenReturn(bookResponse);

        BookResponse result = bookService.update(1L, bookRequest);

        assertNotNull(result);
        verify(bookMapper).updateEntityFromRequest(bookRequest, book);
        verify(bookRepository).save(book);
        verify(bookPriceRepository).save(bookPrice);
    }

    @Test
    void update_WhenBookNotFound_ShouldThrowException() {
        when(bookRepository.findByIdWithPrice(1L)).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class, () -> bookService.update(1L, bookRequest));
        verify(bookRepository).findByIdWithPrice(1L);
        verify(bookRepository, never()).save(any());
    }

    @Test
    void updatePrice_WhenBookExists_ShouldUpdatePrice() {
        UpdatePriceRequest priceRequest = UpdatePriceRequest.builder()
                .priceUsd(new BigDecimal("39.99"))
                .build();

        when(bookRepository.findByIdWithPrice(1L)).thenReturn(Optional.of(book));
        when(bookPriceRepository.save(any(BookPrice.class))).thenReturn(bookPrice);
        when(currencyService.convertUsdToRub(any(BigDecimal.class))).thenReturn(new BigDecimal("2999.25"));
        when(bookMapper.toResponse(eq(book), eq(bookPrice), any(BigDecimal.class))).thenReturn(bookResponse);

        BookResponse result = bookService.updatePrice(1L, priceRequest);

        assertNotNull(result);
        verify(bookMapper).updatePriceFromRequest(priceRequest, bookPrice);
        verify(bookPriceRepository).save(argThat(bp -> bp.getLastPriceUpdate() != null));
    }

    @Test
    void updatePrice_WhenBookNotFound_ShouldThrowException() {
        UpdatePriceRequest priceRequest = UpdatePriceRequest.builder()
                .priceUsd(new BigDecimal("39.99"))
                .build();

        when(bookRepository.findByIdWithPrice(1L)).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class, () -> bookService.updatePrice(1L, priceRequest));
        verify(bookRepository).findByIdWithPrice(1L);
        verify(bookPriceRepository, never()).save(any());
    }

    @Test
    void delete_WhenBookExists_ShouldDeleteBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        doNothing().when(bookRepository).deleteById(1L);

        bookService.delete(1L);

        verify(bookRepository).findById(1L);
        verify(bookRepository).deleteById(1L);
    }

    @Test
    void delete_WhenBookNotFound_ShouldThrowException() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(BookNotFoundException.class, () -> bookService.delete(1L));
        verify(bookRepository).findById(1L);
        verify(bookRepository, never()).deleteById(any());
    }
}

