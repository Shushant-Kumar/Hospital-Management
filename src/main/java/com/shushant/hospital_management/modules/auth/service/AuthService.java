package com.shushant.hospital_management.modules.auth.service;

import com.shushant.hospital_management.modules.auth.dto.AuthResponse;
import com.shushant.hospital_management.modules.auth.dto.ForgotPasswordRequest;
import com.shushant.hospital_management.modules.auth.dto.ForgotPasswordResponse;
import com.shushant.hospital_management.modules.auth.dto.LoginRequest;
import com.shushant.hospital_management.modules.auth.dto.LogoutRequest;
import com.shushant.hospital_management.modules.auth.dto.MessageResponse;
import com.shushant.hospital_management.modules.auth.dto.RegisterRequest;
import com.shushant.hospital_management.modules.auth.dto.ResetPasswordRequest;
import com.shushant.hospital_management.modules.auth.dto.TokenRefreshRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(TokenRefreshRequest request);

    MessageResponse logout(LogoutRequest request);

    ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request);

    MessageResponse resetPassword(ResetPasswordRequest request);
}
