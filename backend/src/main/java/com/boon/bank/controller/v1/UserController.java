package com.boon.bank.controller.v1;

import com.boon.bank.dto.common.ApiResponse;
import com.boon.bank.dto.common.PageResponse;
import com.boon.bank.dto.request.user.UserCreateReq;
import com.boon.bank.dto.response.user.UserRes;
import com.boon.bank.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserRes>> create(@Valid @RequestBody UserCreateReq req) {
        return ResponseEntity.ok(ApiResponse.ok(userService.create(req)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserRes>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserRes>>> list(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(userService.list(pageable))));
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<Void>> enable(@PathVariable UUID id) {
        userService.enable(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Enabled"));
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<Void>> disable(@PathVariable UUID id) {
        userService.disable(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Disabled"));
    }
}
