package com.boon.bank.specification;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class SpecificationBuilder<T> {

    private final List<Specification<T>> specs = new ArrayList<>();

    public static <T> SpecificationBuilder<T> of() {
        return new SpecificationBuilder<>();
    }

    public SpecificationBuilder<T> and(Specification<T> spec) {
        if (spec != null) specs.add(spec);
        return this;
    }

    public Specification<T> build() {
        Specification<T> base = (root, query, cb) -> cb.conjunction();
        return specs.stream().reduce(base, Specification::and);
    }
}
