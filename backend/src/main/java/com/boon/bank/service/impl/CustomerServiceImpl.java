package com.boon.bank.service.impl;

import com.boon.bank.dto.request.CustomerRequest;
import com.boon.bank.dto.response.CustomerResponse;
import com.boon.bank.entity.Customer;
import com.boon.bank.entity.CustomerType;
import com.boon.bank.exception.BusinessException;
import com.boon.bank.exception.ErrorCode;
import com.boon.bank.exception.NotFoundException;
import com.boon.bank.mapper.CustomerMapper;
import com.boon.bank.repository.CustomerRepository;
import com.boon.bank.repository.CustomerTypeRepository;
import com.boon.bank.security.SecurityUtil;
import com.boon.bank.service.CustomerService;
import com.boon.bank.specification.CustomerSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository repo;
    private final CustomerTypeRepository typeRepo;
    private final CustomerMapper mapper;
    private final SecurityUtil securityUtil;

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> findAll(Pageable pageable) {
        if (!securityUtil.isAdmin()) {
            Long custId = securityUtil.currentCustomerId();
            if (custId == null) return new PageImpl<>(List.of(), pageable, 0);
            var customer = repo.findById(custId);
            var list = customer.map(c -> List.of(mapper.toResponse(c))).orElse(List.of());
            return new PageImpl<>(list, pageable, list.size());
        }
        return repo.findAll(pageable).map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> search(String name, String email, String phone, String location, Pageable pageable) {
        var spec = Specification.where(CustomerSpec.nameLike(name))
                .and(CustomerSpec.emailLike(email))
                .and(CustomerSpec.phoneLike(phone))
                .and(CustomerSpec.locationEqual(location));
        return repo.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Override
    @Cacheable(value = "customers", key = "#id")
    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        if (!securityUtil.isAdmin()) {
            Long custId = securityUtil.currentCustomerId();
            if (!id.equals(custId))
                throw new BusinessException(ErrorCode.FORBIDDEN, "Not your account", HttpStatus.FORBIDDEN);
        }
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public CustomerResponse create(CustomerRequest req) {
        if (repo.existsByEmail(req.email()))
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL, "Email already exists", HttpStatus.CONFLICT);
        var entity = mapper.toEntity(req);
        entity.setCustomerType(resolveType(req.customerTypeId()));
        log.info("Customer created: {}", req.email());
        return mapper.toResponse(repo.save(entity));
    }

    @Override
    @CacheEvict(value = "customers", key = "#id")
    @Transactional
    public CustomerResponse update(Long id, CustomerRequest req) {
        var entity = findOrThrow(id);
        mapper.updateFromRequest(req, entity);
        if (req.customerTypeId() != null)
            entity.setCustomerType(resolveType(req.customerTypeId()));
        log.info("Customer updated: id={}", id);
        return mapper.toResponse(repo.save(entity));
    }

    @Override
    @CacheEvict(value = "customers", key = "#id")
    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) throw new NotFoundException("Customer not found");
        repo.deleteById(id);
        log.info("Customer deleted: id={}", id);
    }

    private Customer findOrThrow(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    private CustomerType resolveType(Long typeId) {
        long id = (typeId != null) ? typeId : 1L;
        return typeRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("CustomerType not found"));
    }
}
