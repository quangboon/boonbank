package com.boon.bank.specification;

import com.boon.bank.entity.customer.Customer;
import org.springframework.data.jpa.domain.Specification;

public final class CustomerSpecification {

    private CustomerSpecification() {}

    public static Specification<Customer> hasLocation(String location) {
        return location == null ? null :
                (root, query, cb) -> cb.equal(cb.lower(root.get("location")), location.toLowerCase());
    }

    public static Specification<Customer> hasCustomerType(String code) {
        return code == null ? null :
                (root, query, cb) -> cb.equal(root.get("customerType").get("code"), code);
    }

    public static Specification<Customer> nameContains(String keyword) {
        return keyword == null ? null :
                (root, query, cb) -> cb.like(cb.lower(root.get("fullName")), "%" + keyword.toLowerCase() + "%");
    }
}
