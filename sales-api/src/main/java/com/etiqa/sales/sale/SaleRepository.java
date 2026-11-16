package com.etiqa.sales.sale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    @Query("SELECT s FROM Sale s WHERE s.saleDate >= :startInclusive AND s.saleDate < :endExclusive ORDER BY s.saleDate DESC")
    List<Sale> findSalesInPeriod(
            @Param("startInclusive") Instant startInclusive,
            @Param("endExclusive") Instant endExclusive
    );

    @Query("""
            SELECT s FROM Sale s
            WHERE s.bookId IN (SELECT p.bookId FROM Product p WHERE LOWER(p.bookTitle) LIKE :pattern)
               OR s.customerId IN (
                    SELECT c.customerId FROM Customer c
                    WHERE LOWER(CONCAT(c.firstName, ' ', c.lastName)) LIKE :pattern
               )
            """)
    Page<Sale> search(@Param("pattern") String pattern, Pageable pageable);

    @Query("""
            SELECT new com.etiqa.sales.sale.SalesSummaryResponse(
                COUNT(s), SUM(s.salesQuantity), SUM(s.salesPrice * s.salesQuantity)
            )
            FROM Sale s
            """)
    SalesSummaryResponse summarize();
}
