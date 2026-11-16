package com.etiqa.sales.sale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "sales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sale {

    public static final int MIN_QUANTITY = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_id")
    private Long saleId;

    @Column(name = "sale_date", nullable = false)
    private Instant saleDate;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "sales_quantity", nullable = false)
    private Integer salesQuantity;

    @Column(name = "sales_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal salesPrice;

    @PrePersist
    protected void onCreate() {
        if (this.saleDate == null) {
            this.saleDate = Instant.now();
        }
    }
}
