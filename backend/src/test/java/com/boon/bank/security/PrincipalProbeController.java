package com.boon.bank.security;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PrincipalProbeController {

    @GetMapping("/__probe/customer-id")
    String probe() {
        return SecurityUtil.getCurrentCustomerId()
                .map(UUID::toString)
                .orElse("no-customer");
    }
}
