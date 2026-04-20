package com.boon.bank.repository;

import com.boon.bank.entity.user.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"roles", "customer", "customer.customerType"})
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
