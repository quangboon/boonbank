package com.boon.bank.specification;

import com.boon.bank.entity.transaction.Transaction;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionSpecificationTest {

    @Test
    void involvesAccount_nullCollection_returnsNullSpec() {
        Specification<Transaction> spec = TransactionSpecification.involvesAccount((java.util.Collection<UUID>) null);
        assertThat(spec).isNull();
    }

    @Test
    void involvesAccount_emptyCollection_returnsAlwaysFalsePredicate_notNull() {
        // SECURITY INVARIANT: a non-staff customer with zero accounts must NOT receive any
        // transactions. Before this fix, involvesAccount(emptyList) returned null and the
        // SpecificationBuilder dropped the ownership filter entirely — a full-table leak.
        Specification<Transaction> spec = TransactionSpecification.involvesAccount(Collections.<UUID>emptyList());
        assertThat(spec).isNotNull();

        // Verify it calls cb.disjunction() (always-FALSE). We stub Criteria API mocks
        // just enough to confirm the shape of the call; we do not execute a real query here.
        @SuppressWarnings("unchecked")
        Root<Transaction> root = mock(Root.class);
        @SuppressWarnings("unchecked")
        CriteriaQuery<Object> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate falsePredicate = mock(Predicate.class);
        when(cb.disjunction()).thenReturn(falsePredicate);

        Predicate produced = spec.toPredicate(root, query, cb);
        assertThat(produced).isSameAs(falsePredicate);
        verify(cb).disjunction();
    }

    @Test
    void involvesAccount_nonEmptyCollection_returnsOrPredicate() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        Specification<Transaction> spec = TransactionSpecification.involvesAccount(List.of(id1, id2));
        assertThat(spec).isNotNull();

        @SuppressWarnings("unchecked")
        Root<Transaction> root = mock(Root.class);
        @SuppressWarnings("unchecked")
        Path<Object> sourceAccount = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<Object> destinationAccount = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<Object> sourceId = mock(Path.class);
        @SuppressWarnings("unchecked")
        Path<Object> destinationId = mock(Path.class);
        @SuppressWarnings("unchecked")
        CriteriaBuilder.In<Object> srcIn = mock(CriteriaBuilder.In.class);
        @SuppressWarnings("unchecked")
        CriteriaBuilder.In<Object> dstIn = mock(CriteriaBuilder.In.class);
        when(root.get("sourceAccount")).thenReturn((Path) sourceAccount);
        when(root.get("destinationAccount")).thenReturn((Path) destinationAccount);
        when(sourceAccount.get("id")).thenReturn((Path) sourceId);
        when(destinationAccount.get("id")).thenReturn((Path) destinationId);
        when(sourceId.in(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(srcIn);
        when(destinationId.in(org.mockito.ArgumentMatchers.anyCollection())).thenReturn(dstIn);

        @SuppressWarnings("unchecked")
        CriteriaQuery<Object> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Predicate orPred = mock(Predicate.class);
        when(cb.or(srcIn, dstIn)).thenReturn(orPred);

        assertThat(spec.toPredicate(root, query, cb)).isSameAs(orPred);
        verify(cb).or(srcIn, dstIn);
    }

    @Test
    void hasLocation_nullOrBlank_returnsNullSpec() {
        assertThat(TransactionSpecification.hasLocation(null)).isNull();
        assertThat(TransactionSpecification.hasLocation("")).isNull();
        assertThat(TransactionSpecification.hasLocation("   ")).isNull();
    }

    @Test
    void hasLocation_nonBlank_returnsEqualPredicate() {
        Specification<Transaction> spec = TransactionSpecification.hasLocation("HCM");
        assertThat(spec).isNotNull();
    }
}
