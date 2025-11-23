package com.globus.book_shop.repository;

import com.globus.book_shop.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.bookPrice WHERE b.id = :id")
    Optional<Book> findByIdWithPrice(@Param("id") Long id);

    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.bookPrice")
    Page<Book> findAllWithPrices(Pageable pageable);
}