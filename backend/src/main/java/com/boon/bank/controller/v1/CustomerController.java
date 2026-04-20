package com.boon.bank.controller.v1;

import com.boon.bank.dto.common.ApiResponse;
import com.boon.bank.dto.common.PageResponse;
import com.boon.bank.dto.request.customer.CustomerCreateReq;
import com.boon.bank.dto.request.customer.CustomerUpdateReq;
import com.boon.bank.dto.response.customer.CustomerRes;
import com.boon.bank.service.customer.CustomerService;
import com.boon.bank.service.security.OwnershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final OwnershipService ownershipService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerRes>> create(@Valid @RequestBody CustomerCreateReq req) {
        return ResponseEntity.ok(ApiResponse.ok(customerService.create(req)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CustomerRes>> update(@PathVariable UUID id,
                                                           @Valid @RequestBody CustomerUpdateReq req) {
        return ResponseEntity.ok(ApiResponse.ok(customerService.update(id, req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerRes>> get(@PathVariable UUID id) {
        ownershipService.requireCustomerSelf(id);
        return ResponseEntity.ok(ApiResponse.ok(customerService.getById(id)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<CustomerRes>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String customerTypeCode,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                customerService.search(keyword, location, customerTypeCode, pageable))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        customerService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Deleted"));
    }
}
