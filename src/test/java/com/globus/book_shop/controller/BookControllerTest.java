package com.globus.book_shop.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globus.book_shop.dto.BookRequest;
import com.globus.book_shop.dto.BookResponse;
import com.globus.book_shop.dto.PagedBookResponse;
import com.globus.book_shop.dto.UpdatePriceRequest;
import com.globus.book_shop.exception.BookNotFoundException;
import com.globus.book_shop.service.BookService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookService bookService;

    @Test
    void getAllBooks_ShouldReturnPagedBooks() throws Exception {
        BookResponse bookResponse = BookResponse.builder()
                .id(1L)
                .title("Test Book")
                .author("Test Author")
                .priceUsd(new BigDecimal("29.99"))
                .priceRub(new BigDecimal("2249.25"))
                .build();

        PagedBookResponse pagedResponse = PagedBookResponse.builder()
                .books(List.of(bookResponse))
                .page(0)
                .totalPages(1)
                .totalElements(1)
                .size(10)
                .build();

        when(bookService.findAll(eq(0), eq(10))).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.books[0].id").value(1))
                .andExpect(jsonPath("$.books[0].title").value("Test Book"))
                .andExpect(jsonPath("$.books[0].author").value("Test Author"))
                .andExpect(jsonPath("$.books[0].priceUsd").value(29.99))
                .andExpect(jsonPath("$.books[0].priceRub").value(2249.25))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.page").value(0));

        verify(bookService).findAll(eq(0), eq(10));
    }


    @Test
    void getAllBooks_WithInvalidSize_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/books")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(bookService, never()).findAll(anyInt(), anyInt());
    }

    @Test
    void getBookById_WhenBookExists_ShouldReturnBook() throws Exception {
        BookResponse bookResponse = BookResponse.builder()
                .id(1L)
                .title("Test Book")
                .author("Test Author")
                .priceUsd(new BigDecimal("29.99"))
                .priceRub(new BigDecimal("2249.25"))
                .build();

        when(bookService.findById(1L)).thenReturn(bookResponse);

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Book"))
                .andExpect(jsonPath("$.author").value("Test Author"));

        verify(bookService).findById(1L);
    }

    @Test
    void getBookById_WhenBookNotFound_ShouldReturn404() throws Exception {
        when(bookService.findById(1L)).thenThrow(new BookNotFoundException(1L));

        mockMvc.perform(get("/api/books/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Book Not Found"))
                .andExpect(jsonPath("$.message").value("The requested book was not found"))
                .andExpect(jsonPath("$.path").value("/api/books/1"))
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(bookService).findById(1L);
    }

    @Test
    void createBook_WithValidRequest_ShouldReturnCreated() throws Exception {
        BookRequest bookRequest = BookRequest.builder()
                .title("New Book")
                .author("New Author")
                .priceUsd(new BigDecimal("39.99"))
                .build();

        BookResponse bookResponse = BookResponse.builder()
                .id(1L)
                .title("New Book")
                .author("New Author")
                .priceUsd(new BigDecimal("39.99"))
                .priceRub(new BigDecimal("2999.25"))
                .build();

        when(bookService.create(any(BookRequest.class))).thenReturn(bookResponse);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("New Book"));

        verify(bookService).create(any(BookRequest.class));
    }

    @Test
    void createBook_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        BookRequest invalidRequest = BookRequest.builder()
                .title("")  // Пустой title
                .author("Author")
                .priceUsd(new BigDecimal("39.99"))
                .build();

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value("The request contains invalid data. Please check the validation errors below"))
                .andExpect(jsonPath("$.path").value("/api/books"))
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.validationErrors").isMap())
                .andExpect(jsonPath("$.validationErrors.title").isArray())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(bookService, never()).create(any());
    }

    @Test
    void updateBook_WithValidRequest_ShouldReturnOk() throws Exception {
        BookRequest bookRequest = BookRequest.builder()
                .title("Updated Book")
                .author("Updated Author")
                .priceUsd(new BigDecimal("49.99"))
                .build();

        BookResponse bookResponse = BookResponse.builder()
                .id(1L)
                .title("Updated Book")
                .author("Updated Author")
                .priceUsd(new BigDecimal("49.99"))
                .priceRub(new BigDecimal("3749.25"))
                .build();

        when(bookService.update(eq(1L), any(BookRequest.class))).thenReturn(bookResponse);

        mockMvc.perform(put("/api/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Book"));

        verify(bookService).update(eq(1L), any(BookRequest.class));
    }

    @Test
    void updateBook_WhenBookNotFound_ShouldReturn404() throws Exception {
        BookRequest bookRequest = BookRequest.builder()
                .title("Updated Book")
                .author("Updated Author")
                .priceUsd(new BigDecimal("49.99"))
                .build();

        when(bookService.update(eq(1L), any(BookRequest.class)))
                .thenThrow(new BookNotFoundException(1L));

        mockMvc.perform(put("/api/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Book Not Found"))
                .andExpect(jsonPath("$.message").value("The requested book was not found"))
                .andExpect(jsonPath("$.path").value("/api/books/1"))
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(bookService).update(eq(1L), any(BookRequest.class));
    }

    @Test
    void updatePrice_WithValidRequest_ShouldReturnOk() throws Exception {
        UpdatePriceRequest priceRequest = UpdatePriceRequest.builder()
                .priceUsd(new BigDecimal("59.99"))
                .build();

        BookResponse bookResponse = BookResponse.builder()
                .id(1L)
                .title("Test Book")
                .author("Test Author")
                .priceUsd(new BigDecimal("59.99"))
                .priceRub(new BigDecimal("4499.25"))
                .build();

        when(bookService.updatePrice(eq(1L), any(UpdatePriceRequest.class))).thenReturn(bookResponse);

        mockMvc.perform(patch("/api/books/1/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(priceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceUsd").value(59.99));

        verify(bookService).updatePrice(eq(1L), any(UpdatePriceRequest.class));
    }

    @Test
    void updatePrice_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        UpdatePriceRequest invalidRequest = UpdatePriceRequest.builder()
                .priceUsd(new BigDecimal("-10.00"))  // Отрицательная цена
                .build();

        mockMvc.perform(patch("/api/books/1/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value("The request contains invalid data. Please check the validation errors below"))
                .andExpect(jsonPath("$.path").value("/api/books/1/price"))
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.validationErrors").isMap())
                .andExpect(jsonPath("$.validationErrors.priceUsd").isArray())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(bookService, never()).updatePrice(any(), any());
    }

    @Test
    void deleteBook_WhenBookExists_ShouldReturnNoContent() throws Exception {
        doNothing().when(bookService).delete(1L);

        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isNoContent());

        verify(bookService).delete(1L);
    }

    @Test
    void deleteBook_WhenBookNotFound_ShouldReturn404() throws Exception {
        doThrow(new BookNotFoundException(1L)).when(bookService).delete(1L);

        mockMvc.perform(delete("/api/books/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Book Not Found"))
                .andExpect(jsonPath("$.message").value("The requested book was not found"))
                .andExpect(jsonPath("$.path").value("/api/books/1"))
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.timestamp").exists());

        verify(bookService).delete(1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void getBookById_WithInvalidId_ShouldReturnBadRequest(String id) throws Exception {
        mockMvc.perform(get("/api/books/" + id))
                .andExpect(status().isBadRequest());

        verify(bookService, never()).findById(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void updateBook_WithInvalidId_ShouldReturnBadRequest(String id) throws Exception {
        BookRequest request = BookRequest.builder()
                .title("Test")
                .author("Author")
                .priceUsd(new BigDecimal("10.00"))
                .build();

        mockMvc.perform(put("/api/books/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(bookService, never()).update(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void updatePrice_WithInvalidId_ShouldReturnBadRequest(String id) throws Exception {
        UpdatePriceRequest request = UpdatePriceRequest.builder()
                .priceUsd(new BigDecimal("10.00"))
                .build();

        mockMvc.perform(patch("/api/books/" + id + "/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(bookService, never()).updatePrice(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void deleteBook_WithInvalidId_ShouldReturnBadRequest(String id) throws Exception {
        mockMvc.perform(delete("/api/books/" + id))
                .andExpect(status().isBadRequest());

        verify(bookService, never()).delete(any());
    }

    @Test
    void getAllBooks_WithValidPage_ShouldReturnOk() throws Exception {
        PagedBookResponse pagedResponse = PagedBookResponse.builder()
                .books(List.of())
                .page(0)
                .totalPages(0)
                .totalElements(0)
                .size(10)
                .build();

        when(bookService.findAll(eq(0), anyInt())).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/books").param("page", "0"))
                .andExpect(status().isOk());

        verify(bookService).findAll(eq(0), anyInt());
    }

    @Test
    void getAllBooks_WithInvalidPage_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/books").param("page", "-1"))
                .andExpect(status().isBadRequest());

        verify(bookService, never()).findAll(anyInt(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 100})
    void getAllBooks_WithValidSize_ShouldReturnOk(int size) throws Exception {
        PagedBookResponse pagedResponse = PagedBookResponse.builder()
                .books(List.of())
                .page(0)
                .totalPages(0)
                .totalElements(0)
                .size(size)
                .build();

        when(bookService.findAll(anyInt(), eq(size))).thenReturn(pagedResponse);

        mockMvc.perform(get("/api/books").param("size", String.valueOf(size)))
                .andExpect(status().isOk());

        verify(bookService).findAll(anyInt(), eq(size));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101})
    void getAllBooks_WithInvalidSize_ShouldReturnBadRequest(int size) throws Exception {
        mockMvc.perform(get("/api/books").param("size", String.valueOf(size)))
                .andExpect(status().isBadRequest());

        verify(bookService, never()).findAll(anyInt(), anyInt());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void createBook_WithInvalidTitle_ShouldReturnBadRequest(String title) throws Exception {
        BookRequest request = BookRequest.builder()
                .title(title)
                .author("Author")
                .priceUsd(new BigDecimal("10.00"))
                .build();

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(bookService, never()).create(any());
    }

    @Test
    void createBook_WithTitleMaxLength_ShouldReturnCreated() throws Exception {
        String maxTitle = "a".repeat(255);
        BookRequest request = BookRequest.builder()
                .title(maxTitle)
                .author("Author")
                .priceUsd(new BigDecimal("10.00"))
                .build();

        BookResponse response = BookResponse.builder()
                .id(1L)
                .title(maxTitle)
                .author("Author")
                .priceUsd(new BigDecimal("10.00"))
                .priceRub(new BigDecimal("750.00"))
                .build();

        when(bookService.create(any(BookRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(bookService).create(any(BookRequest.class));
    }

    @Test
    void createBook_WithTitleOverMaxLength_ShouldReturnBadRequest() throws Exception {
        String overMaxTitle = "a".repeat(256);
        BookRequest request = BookRequest.builder()
                .title(overMaxTitle)
                .author("Author")
                .priceUsd(new BigDecimal("10.00"))
                .build();

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(bookService, never()).create(any());
    }

    @Test
    void createBook_WithAuthorOverMaxLength_ShouldReturnBadRequest() throws Exception {
        String overMaxAuthor = "a".repeat(256);
        BookRequest request = BookRequest.builder()
                .title("Title")
                .author(overMaxAuthor)
                .priceUsd(new BigDecimal("10.00"))
                .build();

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(bookService, never()).create(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void createBook_WithInvalidAuthor_ShouldReturnBadRequest(String author) throws Exception {
        BookRequest request = BookRequest.builder()
                .title("Title")
                .author(author)
                .priceUsd(new BigDecimal("10.00"))
                .build();

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(bookService, never()).create(any());
    }

    @Test
    void createBook_WithInvalidPrice_ShouldReturnBadRequest() throws Exception {
        BookRequest request = BookRequest.builder()
                .title("Title")
                .author("Author")
                .priceUsd(new BigDecimal("-10.00"))
                .build();

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(bookService, never()).create(any());
    }

    @Test
    void createBook_WithPriceBoundaryValues_ShouldReturnCreated() throws Exception {
        BookRequest minRequest = BookRequest.builder()
                .title("Title")
                .author("Author")
                .priceUsd(new BigDecimal("0.01"))
                .build();

        BookRequest maxRequest = BookRequest.builder()
                .title("Title")
                .author("Author")
                .priceUsd(new BigDecimal("999999.99"))
                .build();

        BookResponse response = BookResponse.builder()
                .id(1L)
                .title("Title")
                .author("Author")
                .priceUsd(new BigDecimal("0.01"))
                .priceRub(new BigDecimal("0.75"))
                .build();

        when(bookService.create(any(BookRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minRequest)))
                .andExpect(status().isCreated());

        BookResponse maxResponse = BookResponse.builder()
                .id(1L)
                .title("Title")
                .author("Author")
                .priceUsd(new BigDecimal("999999.99"))
                .priceRub(new BigDecimal("74999992.50"))
                .build();
        when(bookService.create(any(BookRequest.class))).thenReturn(maxResponse);

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maxRequest)))
                .andExpect(status().isCreated());

        verify(bookService, times(2)).create(any(BookRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.009", "1000000.00"})
    void createBook_WithPriceOutOfRange_ShouldReturnBadRequest(String price) throws Exception {
        BookRequest request = BookRequest.builder()
                .title("Title")
                .author("Author")
                .priceUsd(new BigDecimal(price))
                .build();

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(bookService, never()).create(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-10.00", "0.009", "1000000.00"})
    void updatePrice_WithInvalidPrice_ShouldReturnBadRequest(String price) throws Exception {
        UpdatePriceRequest request = UpdatePriceRequest.builder()
                .priceUsd(new BigDecimal(price))
                .build();

        mockMvc.perform(patch("/api/books/1/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(bookService, never()).updatePrice(any(), any());
    }

    @Test
    void updatePrice_WithValidPriceBoundaries_ShouldReturnOk() throws Exception {
        UpdatePriceRequest minRequest = UpdatePriceRequest.builder()
                .priceUsd(new BigDecimal("0.01"))
                .build();

        UpdatePriceRequest maxRequest = UpdatePriceRequest.builder()
                .priceUsd(new BigDecimal("999999.99"))
                .build();

        BookResponse response = BookResponse.builder()
                .id(1L)
                .title("Test")
                .author("Author")
                .priceUsd(new BigDecimal("0.01"))
                .priceRub(new BigDecimal("0.75"))
                .build();

        when(bookService.updatePrice(eq(1L), any(UpdatePriceRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/books/1/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minRequest)))
                .andExpect(status().isOk());

        BookResponse maxResponse = BookResponse.builder()
                .id(1L)
                .title("Test")
                .author("Author")
                .priceUsd(new BigDecimal("999999.99"))
                .priceRub(new BigDecimal("74999992.50"))
                .build();
        when(bookService.updatePrice(eq(1L), any(UpdatePriceRequest.class))).thenReturn(maxResponse);

        mockMvc.perform(patch("/api/books/1/price")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maxRequest)))
                .andExpect(status().isOk());

        verify(bookService, times(2)).updatePrice(eq(1L), any(UpdatePriceRequest.class));
    }

    @Test
    void createBook_WithAllFieldsInvalid_ShouldReturnBadRequest() throws Exception {
        BookRequest request = BookRequest.builder()
                .title("")
                .author("")
                .priceUsd(new BigDecimal("-1.00"))
                .build();

        mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value("The request contains invalid data. Please check the validation errors below"))
                .andExpect(jsonPath("$.path").value("/api/books"))
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.validationErrors").isMap())
                .andExpect(jsonPath("$.validationErrors.title").isArray())
                .andExpect(jsonPath("$.validationErrors.author").isArray())
                .andExpect(jsonPath("$.validationErrors.priceUsd").isArray())
                .andExpect(jsonPath("$.validationErrors.title.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.validationErrors.author.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.validationErrors.priceUsd.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(bookService, never()).create(any());
    }

    @Test
    void updateBook_WithAllFieldsInvalid_ShouldReturnBadRequest() throws Exception {
        BookRequest request = BookRequest.builder()
                .title("")
                .author("")
                .priceUsd(new BigDecimal("-1.00"))
                .build();

        mockMvc.perform(put("/api/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.message").value("The request contains invalid data. Please check the validation errors below"))
                .andExpect(jsonPath("$.path").value("/api/books/1"))
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.validationErrors").isMap())
                .andExpect(jsonPath("$.validationErrors.title").isArray())
                .andExpect(jsonPath("$.validationErrors.author").isArray())
                .andExpect(jsonPath("$.validationErrors.priceUsd").isArray())
                .andExpect(jsonPath("$.validationErrors.title.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.validationErrors.author.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.validationErrors.priceUsd.length()").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(bookService, never()).update(any(), any());
    }
}