package com.boon.bank.controller.v1;

import com.boon.bank.common.idempotency.IdempotentInterceptor;
import com.boon.bank.dto.common.ApiResponse;
import com.boon.bank.dto.common.PageResponse;
import com.boon.bank.dto.request.transaction.DepositReq;
import com.boon.bank.dto.request.transaction.TransactionSearchReq;
import com.boon.bank.dto.request.transaction.TransferReq;
import com.boon.bank.dto.request.transaction.WithdrawReq;
import com.boon.bank.dto.response.transaction.TransactionRes;
import com.boon.bank.exception.business.ForbiddenException;
import com.boon.bank.repository.AccountRepository;
import com.boon.bank.security.SecurityUtil;
import com.boon.bank.service.security.OwnershipService;
import com.boon.bank.service.transaction.TransactionQueryService;
import com.boon.bank.service.transaction.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionQueryService transactionQueryService;
    private final OwnershipService ownershipService;
    private final AccountRepository accountRepository;

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionRes>> transfer(
            @Valid @RequestBody TransferReq req,
            @RequestHeader(value = IdempotentInterceptor.HEADER, required = false) String idempotencyKey) {
        ownershipService.requireAccountNumberOwned(req.sourceAccountNumber());
        return ResponseEntity.ok(ApiResponse.ok(transactionService.transfer(req, idempotencyKey)));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<TransactionRes>> withdraw(
            @Valid @RequestBody WithdrawReq req,
            @RequestHeader(value = IdempotentInterceptor.HEADER, required = false) String idempotencyKey) {
        ownershipService.requireAccountNumberOwned(req.accountNumber());
        return ResponseEntity.ok(ApiResponse.ok(transactionService.withdraw(req, idempotencyKey)));
    }

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<TransactionRes>> deposit(
            @Valid @RequestBody DepositReq req,
            @RequestHeader(value = IdempotentInterceptor.HEADER, required = false) String idempotencyKey) {
        ownershipService.requireAccountNumberOwned(req.accountNumber());
        return ResponseEntity.ok(ApiResponse.ok(transactionService.deposit(req, idempotencyKey)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TransactionRes>>> search(
            @ModelAttribute TransactionSearchReq req,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        List<UUID> ownershipScope = resolveOwnershipScope(req.accountId());
        return ResponseEntity.ok(ApiResponse.ok(
                PageResponse.from(transactionQueryService.search(req, ownershipScope, pageable))));
    }

    private List<UUID> resolveOwnershipScope(UUID requestedAccountId) {
        if (ownershipService.isStaff()) {
            return null; // staff full-scan; query uses accountId filter if provided
        }
        UUID customerId = SecurityUtil.getCurrentCustomerId()
                .orElseThrow(ForbiddenException::new);
        List<UUID> ownedIds = accountRepository.findIdsByCustomerId(customerId);
        if (requestedAccountId != null && !ownedIds.contains(requestedAccountId)) {
            throw new ForbiddenException("Account does not belong to current user");
        }
        return ownedIds;
    }
}
