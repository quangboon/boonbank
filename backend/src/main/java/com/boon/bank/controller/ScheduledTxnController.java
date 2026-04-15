package com.boon.bank.controller;

import com.boon.bank.dto.request.ScheduledTxnRequest;
import com.boon.bank.dto.response.ApiResponse;
import com.boon.bank.dto.response.ScheduledTxnResponse;
import com.boon.bank.entity.ScheduledTransaction;
import com.boon.bank.service.ScheduledTxnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scheduled-transactions")
@RequiredArgsConstructor
public class ScheduledTxnController {

    private final ScheduledTxnService service;

    @GetMapping
    ApiResponse<Page<ScheduledTxnResponse>> list(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(service.findAll(pageable).map(this::toResponse));
    }

    @GetMapping("/{uuid}")
    ApiResponse<ScheduledTxnResponse> getByUuid(@PathVariable UUID uuid) {
        return ApiResponse.ok(toResponse(service.getByUuid(uuid)));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<ScheduledTxnResponse> create(@Valid @RequestBody ScheduledTxnRequest req) {
        var entity = service.create(req.accountId(), req.toAccountId(),
                req.type(), req.amount(), req.cronExpression(), req.description());
        return ApiResponse.ok(toResponse(entity));
    }

    @PutMapping("/{uuid}/active")
    ApiResponse<ScheduledTxnResponse> toggle(@PathVariable UUID uuid,
                                              @RequestParam boolean active) {
        return ApiResponse.ok(toResponse(service.toggle(uuid, active)));
    }

    @DeleteMapping("/{uuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID uuid) {
        service.delete(uuid);
    }

    private ScheduledTxnResponse toResponse(ScheduledTransaction s) {
        return new ScheduledTxnResponse(
                s.getUuid().toString(),
                s.getAccount().getId(),
                s.getToAccount() != null ? s.getToAccount().getId() : null,
                s.getType(), s.getAmount(),
                s.getCronExpression(), s.getDescription(),
                s.getActive(), s.getNextRunAt(),
                s.getLastRunAt(), s.getCreatedAt()
        );
    }
}
