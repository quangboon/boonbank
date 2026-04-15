package com.boon.bank.controller;

import com.boon.bank.dto.request.TransactionRequest;
import com.boon.bank.dto.response.ApiResponse;
import com.boon.bank.dto.response.TransactionResponse;
import com.boon.bank.entity.enums.TransactionType;
import com.boon.bank.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService txnService;

    @Value("${app.timezone:Asia/Ho_Chi_Minh}")
    private String timezone;

    @GetMapping
    ApiResponse<Page<TransactionResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(txnService.findAll(pageable));
    }

    @GetMapping("/search")
    ApiResponse<Page<TransactionResponse>> search(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) BigDecimal amountMin,
            @RequestParam(required = false) BigDecimal amountMax,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @PageableDefault(size = 20) Pageable pageable) {
        var zone = java.time.ZoneId.of(timezone);
        OffsetDateTime fromDt = from != null ? LocalDate.parse(from).atStartOfDay(zone).toOffsetDateTime() : null;
        OffsetDateTime toDt = to != null ? LocalDate.parse(to).plusDays(1).atStartOfDay(zone).toOffsetDateTime() : null;
        return ApiResponse.ok(txnService.search(type, amountMin, amountMax, fromDt, toDt, pageable));
    }

    @GetMapping("/{id}")
    ApiResponse<TransactionResponse> getById(@PathVariable Long id) {
        return ApiResponse.ok(txnService.getById(id));
    }

    @GetMapping("/account/{accountId}")
    ApiResponse<Page<TransactionResponse>> byAccount(@PathVariable Long accountId,
                                                      @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(txnService.findByAccount(accountId, pageable));
    }

    @GetMapping("/fee-preview")
    ApiResponse<Map<String, BigDecimal>> feePreview(@RequestParam TransactionType type,
                                                     @RequestParam BigDecimal amount) {
        var fee = txnService.previewFee(type, amount);
        var total = amount.add(fee);
        return ApiResponse.ok(Map.of("fee", fee, "total", total));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<TransactionResponse> execute(@Valid @RequestBody TransactionRequest req) {
        return ApiResponse.ok(txnService.execute(req));
    }
}
