package com.etiqa.sales.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmailOffice(String emailOffice);

    boolean existsByEmailPersonal(String emailPersonal);

    boolean existsByEmailOfficeAndCustomerIdNot(String emailOffice, Long customerId);

    boolean existsByEmailPersonalAndCustomerIdNot(String emailPersonal, Long customerId);

    @Query("""
            SELECT c FROM Customer c
            WHERE LOWER(CONCAT(c.firstName, ' ', c.lastName)) LIKE :pattern
               OR LOWER(c.emailOffice) LIKE :pattern
               OR LOWER(c.emailPersonal) LIKE :pattern
            """)
    Page<Customer> search(@Param("pattern") String pattern, Pageable pageable);
}
