package com.boon.bank.controller;

import com.boon.bank.dto.request.CustomerRequest;
import com.boon.bank.dto.response.ApiResponse;
import com.boon.bank.dto.response.CustomerResponse;
import com.boon.bank.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @GetMapping
    ApiResponse<Page<CustomerResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(customerService.findAll(pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<Page<CustomerResponse>> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String location,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(customerService.search(name, email, phone, location, pageable));
    }

    @GetMapping("/{id}")
    ApiResponse<CustomerResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(customerService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<CustomerResponse> create(@Valid @RequestBody CustomerRequest req) {
        return ApiResponse.ok(customerService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<CustomerResponse> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest req) {
        return ApiResponse.ok(customerService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    void delete(@PathVariable Long id) { customerService.delete(id); }
}
