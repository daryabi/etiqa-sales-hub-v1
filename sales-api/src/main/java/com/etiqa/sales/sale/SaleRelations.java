package com.etiqa.sales.sale;

import com.etiqa.sales.customer.Customer;
import com.etiqa.sales.product.Product;
import java.util.Map;

public record SaleRelations(Map<Long, Product> booksById, Map<Long, Customer> customersById) {

    public Product bookOf(Sale sale) {
        Product book = booksById.get(sale.getBookId());
        if (book == null) {
            throw new IllegalStateException("Sale " + sale.getSaleId() + " refers to missing book " + sale.getBookId());
        }
        return book;
    }

    public Customer customerOf(Sale sale) {
        Customer customer = customersById.get(sale.getCustomerId());
        if (customer == null) {
            throw new IllegalStateException("Sale " + sale.getSaleId() + " refers to missing customer " + sale.getCustomerId());
        }
        return customer;
    }
}
