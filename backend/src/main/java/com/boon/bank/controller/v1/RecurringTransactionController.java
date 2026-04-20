package com.boon.bank.controller.v1;

import com.boon.bank.dto.common.ApiResponse;
import com.boon.bank.dto.common.PageResponse;
import com.boon.bank.dto.request.recurring.RecurringTransactionCreateReq;
import com.boon.bank.dto.request.recurring.RecurringTransactionUpdateReq;
import com.boon.bank.dto.response.recurring.RecurringTransactionRes;
import com.boon.bank.service.recurring.RecurringTransactionService;
import com.boon.bank.service.security.OwnershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/recurring-transactions")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;
    private final OwnershipService ownershipService;

    @PostMapping
    public ResponseEntity<ApiResponse<RecurringTransactionRes>> create(
            @Valid @RequestBody RecurringTransactionCreateReq req) {
        ownershipService.requireAccountNumberOwned(req.sourceAccountNumber());
        return ResponseEntity.ok(ApiResponse.ok(recurringTransactionService.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringTransactionRes>> update(
            @PathVariable UUID id,
            @Valid @RequestBody RecurringTransactionUpdateReq req) {
        ownershipService.requireRecurringOwned(id);
        return ResponseEntity.ok(ApiResponse.ok(recurringTransactionService.update(id, req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecurringTransactionRes>> get(@PathVariable UUID id) {
        ownershipService.requireRecurringOwned(id);
        return ResponseEntity.ok(ApiResponse.ok(recurringTransactionService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RecurringTransactionRes>>> search(
            @RequestParam(required = false) UUID sourceAccountId,
            @RequestParam(required = false) Boolean enabled,
            Pageable pageable) {
        if (sourceAccountId != null) {
            ownershipService.requireAccountOwned(sourceAccountId);
        }
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(
                recurringTransactionService.search(sourceAccountId, enabled, pageable))));
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<Void>> enable(@PathVariable UUID id) {
        ownershipService.requireRecurringOwned(id);
        recurringTransactionService.enable(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Enabled"));
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<Void>> disable(@PathVariable UUID id) {
        ownershipService.requireRecurringOwned(id);
        recurringTransactionService.disable(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Disabled"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        ownershipService.requireRecurringOwned(id);
        recurringTransactionService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Deleted"));
    }
}
