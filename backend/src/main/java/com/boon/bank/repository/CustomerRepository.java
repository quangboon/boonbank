package com.boon.bank.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.boon.bank.entity.customer.Customer;
import com.boon.bank.repository.projection.LocationCustomerCount;

public interface CustomerRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByCustomerCode(String customerCode);

    Optional<Customer> findByIdNumber(String idNumber);

    boolean existsByEmail(String email);

    @Query("""
            select c.location as city, count(c) as customerCount
            from Customer c
            where c.deletedAt is null and c.location is not null
            group by c.location
            order by customerCount desc
            """)
    List<LocationCustomerCount> groupByLocation();
}
