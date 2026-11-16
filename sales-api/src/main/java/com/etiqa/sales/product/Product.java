package com.etiqa.sales.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    public static final int TITLE_MAX_LENGTH = 255;
    public static final String MIN_PRICE = "0.01";
    public static final int MIN_STOCK = 0;

    // book* names are deliberate: the spec calls these columns Book_ID, Book_Title, Book_Price, Book_Stock_Quantity.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long bookId;

    @Column(name = "book_title", nullable = false, length = TITLE_MAX_LENGTH)
    private String bookTitle;

    @Column(name = "book_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal bookPrice;

    @Column(name = "book_stock_quantity", nullable = false)
    private Integer bookStockQuantity;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
