package com.boon.bank.repository;

import com.boon.bank.entity.customer.CustomerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerTypeRepository extends JpaRepository<CustomerType, UUID> {
    Optional<CustomerType> findByCode(String code);
}
