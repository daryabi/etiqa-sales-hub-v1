package com.etiqa.sales.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Page<Product> findByActiveTrue(Pageable pageable);

    Page<Product> findByActiveTrueAndBookTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query("""
            SELECT new com.etiqa.sales.product.InventorySummaryResponse(COUNT(p), SUM(p.bookStockQuantity))
            FROM Product p
            WHERE p.active = true
            """)
    InventorySummaryResponse summarizeActiveInventory();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE Product p
            SET p.bookStockQuantity = p.bookStockQuantity + :quantity,
                p.version = p.version + 1,
                p.updatedAt = :updatedAt
            WHERE p.bookId = :bookId
            """)
    int increaseStock(
            @Param("bookId") Long bookId,
            @Param("quantity") int quantity,
            @Param("updatedAt") Instant updatedAt
    );

    @Query("SELECT LOWER(p.bookTitle) FROM Product p WHERE LOWER(p.bookTitle) IN :lowerCaseTitles")
    List<String> findExistingTitles(@Param("lowerCaseTitles") Collection<String> lowerCaseTitles);
}
