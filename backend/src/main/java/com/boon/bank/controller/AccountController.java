package com.boon.bank.controller;

import com.boon.bank.dto.request.AccountRequest;
import com.boon.bank.dto.response.AccountResponse;
import com.boon.bank.dto.response.ApiResponse;
import com.boon.bank.dto.response.StatusHistoryResponse;
import com.boon.bank.entity.enums.AccountStatus;
import com.boon.bank.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    ApiResponse<Page<AccountResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(accountService.findAll(pageable));
    }

    @GetMapping("/lookup")
    ApiResponse<AccountResponse> lookup(@RequestParam String accountNumber) {
        return ApiResponse.ok(accountService.lookupByNumber(accountNumber));
    }

    @GetMapping("/{id:\\d+}")
    ApiResponse<AccountResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(accountService.getById(id));
    }

    @GetMapping("/customer/{customerId}")
    ApiResponse<Page<AccountResponse>> byCustomer(@PathVariable Long customerId,
                                                   @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(accountService.findByCustomer(customerId, pageable));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<AccountResponse> create(@Valid @RequestBody AccountRequest req) {
        return ApiResponse.ok(accountService.create(req));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/status")
    ApiResponse<AccountResponse> changeStatus(@PathVariable Long id,
                                               @RequestParam AccountStatus status,
                                               @RequestParam(required = false) String reason,
                                               @RequestParam(defaultValue = "system") String changedBy) {
        return ApiResponse.ok(accountService.changeStatus(id, status, reason, changedBy));
    }

    @GetMapping("/{id}/status-history")
    ApiResponse<Page<StatusHistoryResponse>> statusHistory(@PathVariable Long id,
                                                            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(accountService.getStatusHistory(id, pageable));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    void delete(@PathVariable Long id) { accountService.delete(id); }
}
