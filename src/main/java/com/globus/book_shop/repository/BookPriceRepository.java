package com.globus.book_shop.repository;

import com.globus.book_shop.entity.BookPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface BookPriceRepository extends JpaRepository<BookPrice, Long> {

    Optional<BookPrice> findByBookId(Long bookId);

    @Modifying
    @Query("UPDATE BookPrice bp SET bp.priceUsd = :priceUsd, bp.lastPriceUpdate = :lastPriceUpdate WHERE bp.id = :bookId")
    void updatePrice(@Param("bookId") Long bookId,
                     @Param("priceUsd") BigDecimal priceUsd,
                     @Param("lastPriceUpdate") LocalDateTime lastPriceUpdate);

    boolean existsByBookId(Long bookId);
}