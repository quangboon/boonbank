package com.boon.bank.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String code,
        String traceId,
        Instant timestamp,
        List<FieldError> errors
) {
    public record FieldError(String field, String message) {}
}
