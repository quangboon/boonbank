package com.boon.bank.repository;

import com.boon.bank.entity.Customer;
import com.boon.bank.repository.projection.LocationStatsProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long>,
        JpaSpecificationExecutor<Customer> {
    boolean existsByEmail(String email);

    @Query(value = "SELECT COALESCE(location, 'Unknown') AS location, COUNT(*) AS customer_count " +
            "FROM customer GROUP BY location ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<LocationStatsProjection> findCustomersByLocation();
}
