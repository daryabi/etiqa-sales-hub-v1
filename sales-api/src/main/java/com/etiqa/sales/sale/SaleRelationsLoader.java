package com.etiqa.sales.sale;

import com.etiqa.sales.customer.Customer;
import com.etiqa.sales.customer.CustomerService;
import com.etiqa.sales.product.Product;
import com.etiqa.sales.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SaleRelationsLoader {

    private final ProductService productService;
    private final CustomerService customerService;

    @Transactional(readOnly = true)
    public SaleRelations loadFor(List<Sale> sales) {
        if (sales.isEmpty()) {
            return new SaleRelations(Map.of(), Map.of());
        }
        List<Long> bookIds = sales.stream().map(Sale::getBookId).distinct().toList();
        List<Long> customerIds = sales.stream().map(Sale::getCustomerId).distinct().toList();

        Map<Long, Product> booksById = productService.findAllByIds(bookIds).stream()
                .collect(Collectors.toMap(Product::getBookId, Function.identity()));
        Map<Long, Customer> customersById = customerService.findAllByIds(customerIds).stream()
                .collect(Collectors.toMap(Customer::getCustomerId, Function.identity()));
        return new SaleRelations(booksById, customersById);
    }
}
