package com.boon.bank.specification;

import com.boon.bank.entity.Customer;
import org.springframework.data.jpa.domain.Specification;

public class CustomerSpec {

    public static Specification<Customer> nameLike(String name) {
        return (root, q, cb) -> name == null || name.isBlank() ? null :
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Customer> emailLike(String email) {
        return (root, q, cb) -> email == null || email.isBlank() ? null :
                cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
    }

    public static Specification<Customer> phoneLike(String phone) {
        return (root, q, cb) -> phone == null || phone.isBlank() ? null :
                cb.like(root.get("phone"), "%" + phone + "%");
    }

    public static Specification<Customer> locationEqual(String location) {
        return (root, q, cb) -> location == null || location.isBlank() ? null :
                cb.equal(cb.lower(root.get("location")), location.toLowerCase());
    }
}
