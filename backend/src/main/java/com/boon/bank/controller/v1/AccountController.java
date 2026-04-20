package com.boon.bank.controller.v1;

import com.boon.bank.dto.common.ApiResponse;
import com.boon.bank.dto.common.PageResponse;
import com.boon.bank.dto.request.account.AccountCreateReq;
import com.boon.bank.dto.request.account.AccountSearchReq;
import com.boon.bank.dto.request.account.AccountUpdateReq;
import com.boon.bank.dto.response.account.AccountBalanceRes;
import com.boon.bank.dto.response.account.AccountRes;
import com.boon.bank.dto.response.account.AccountStatusHistoryRes;
import com.boon.bank.service.account.AccountService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final OwnershipService ownershipService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountRes>> open(@Valid @RequestBody AccountCreateReq req) {
        return ResponseEntity.ok(ApiResponse.ok(accountService.open(req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountRes>> get(@PathVariable UUID id) {
        ownershipService.requireAccountOwned(id);
        return ResponseEntity.ok(ApiResponse.ok(accountService.getById(id)));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<ApiResponse<AccountBalanceRes>> balance(@PathVariable String accountNumber) {
        ownershipService.requireAccountNumberOwned(accountNumber);
        return ResponseEntity.ok(ApiResponse.ok(accountService.getBalance(accountNumber)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AccountRes>>> search(AccountSearchReq req, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(accountService.search(req, pageable))));
    }

    @GetMapping("/{id}/status-history")
    public ResponseEntity<ApiResponse<List<AccountStatusHistoryRes>>> statusHistory(@PathVariable UUID id) {
        ownershipService.requireAccountOwned(id);
        return ResponseEntity.ok(ApiResponse.ok(accountService.getStatusHistory(id)));
    }

    @PostMapping("/{id}/freeze")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountRes>> freeze(@PathVariable UUID id,
                                                          @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.ok(accountService.freeze(id, reason)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountRes>> close(@PathVariable UUID id,
                                                         @RequestParam String reason) {
        return ResponseEntity.ok(ApiResponse.ok(accountService.close(id, reason)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountRes>> update(@PathVariable UUID id,
                                                          @Valid @RequestBody AccountUpdateReq req) {
        return ResponseEntity.ok(ApiResponse.ok(accountService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        accountService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
