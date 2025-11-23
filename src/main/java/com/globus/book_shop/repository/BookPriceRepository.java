package com.globus.book_shop.repository;

import com.globus.book_shop.entity.BookPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookPriceRepository extends JpaRepository<BookPrice, Long> {

    Optional<BookPrice> findByBookId(Long bookId);

    boolean existsByBookId(Long bookId);
}