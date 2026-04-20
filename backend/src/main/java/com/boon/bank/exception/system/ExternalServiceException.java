package com.boon.bank.exception.system;

import com.boon.bank.exception.ErrorCode;
import lombok.Getter;

@Getter
public class ExternalServiceException extends RuntimeException {

    private final ErrorCode errorCode = ErrorCode.EXTERNAL_SERVICE_ERROR;
    private final String service;

    public ExternalServiceException(String service, String message) {
        super(message);
        this.service = service;
    }

    public ExternalServiceException(String service, String message, Throwable cause) {
        super(message, cause);
        this.service = service;
    }
}
