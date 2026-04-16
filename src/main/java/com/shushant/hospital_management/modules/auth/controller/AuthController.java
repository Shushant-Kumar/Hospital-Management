package com.shushant.hospital_management.modules.auth.controller;

import com.shushant.hospital_management.common.dto.ApiResponse;
import com.shushant.hospital_management.modules.auth.dto.AuthResponse;
import com.shushant.hospital_management.modules.auth.dto.ForgotPasswordRequest;
import com.shushant.hospital_management.modules.auth.dto.ForgotPasswordResponse;
import com.shushant.hospital_management.modules.auth.dto.LoginRequest;
import com.shushant.hospital_management.modules.auth.dto.LogoutRequest;
import com.shushant.hospital_management.modules.auth.dto.MessageResponse;
import com.shushant.hospital_management.modules.auth.dto.RegisterRequest;
import com.shushant.hospital_management.modules.auth.dto.ResetPasswordRequest;
import com.shushant.hospital_management.modules.auth.dto.TokenRefreshRequest;
import com.shushant.hospital_management.modules.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(authService.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refreshToken(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(@Valid @RequestBody LogoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.logout(request)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<ForgotPasswordResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.forgotPassword(request)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<MessageResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.resetPassword(request)));
    }
}
