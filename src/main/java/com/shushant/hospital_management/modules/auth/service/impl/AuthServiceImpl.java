package com.shushant.hospital_management.modules.auth.service.impl;

import com.shushant.hospital_management.common.exception.BusinessException;
import com.shushant.hospital_management.common.exception.ErrorCode;
import com.shushant.hospital_management.common.exception.ResourceConflictException;
import com.shushant.hospital_management.common.util.HashingUtil;
import com.shushant.hospital_management.modules.auth.config.AuthProperties;
import com.shushant.hospital_management.modules.auth.dto.AuthResponse;
import com.shushant.hospital_management.modules.auth.dto.AuthUserResponse;
import com.shushant.hospital_management.modules.auth.dto.ForgotPasswordRequest;
import com.shushant.hospital_management.modules.auth.dto.ForgotPasswordResponse;
import com.shushant.hospital_management.modules.auth.dto.LoginRequest;
import com.shushant.hospital_management.modules.auth.dto.LogoutRequest;
import com.shushant.hospital_management.modules.auth.dto.MessageResponse;
import com.shushant.hospital_management.modules.auth.dto.RegisterRequest;
import com.shushant.hospital_management.modules.auth.dto.ResetPasswordRequest;
import com.shushant.hospital_management.modules.auth.dto.TokenRefreshRequest;
import com.shushant.hospital_management.modules.auth.entity.AppUser;
import com.shushant.hospital_management.modules.auth.entity.PasswordResetToken;
import com.shushant.hospital_management.modules.auth.entity.RefreshToken;
import com.shushant.hospital_management.modules.auth.entity.Role;
import com.shushant.hospital_management.modules.auth.entity.RoleName;
import com.shushant.hospital_management.modules.auth.repository.AppUserRepository;
import com.shushant.hospital_management.modules.auth.repository.PasswordResetTokenRepository;
import com.shushant.hospital_management.modules.auth.repository.RefreshTokenRepository;
import com.shushant.hospital_management.modules.auth.repository.RoleRepository;
import com.shushant.hospital_management.modules.auth.service.AuthService;
import com.shushant.hospital_management.security.jwt.JwtProperties;
import com.shushant.hospital_management.security.jwt.JwtTokenService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final long PASSWORD_RESET_TOKEN_MINUTES = 30;

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;
    private final AuthProperties authProperties;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(
            AppUserRepository appUserRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            JwtTokenService jwtTokenService,
            JwtProperties jwtProperties,
            AuthProperties authProperties,
            PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.jwtTokenService = jwtTokenService;
        this.jwtProperties = jwtProperties;
        this.authProperties = authProperties;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (appUserRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ResourceConflictException("A user with this email already exists");
        }

        Role defaultRole = roleRepository.findByName(RoleName.ROLE_PATIENT)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Default role is missing"));

        AppUser user = new AppUser();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.getRoles().add(defaultRole);

        AppUser savedUser = appUserRepository.save(user);
        return issueTokens(savedUser);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByEmailIgnoreCaseAndDeletedFalse(request.email().trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Invalid credentials"));

        validateAccountLock(user);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedAttempt(user);
            throw new BusinessException(ErrorCode.AUTHENTICATION_FAILED, "Invalid credentials");
        }

        resetFailedAttempts(user);
        return issueTokens(user);
    }

    @Override
    public AuthResponse refreshToken(TokenRefreshRequest request) {
        String hashedToken = HashingUtil.sha256(request.refreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, "Refresh token is invalid"));

        OffsetDateTime now = OffsetDateTime.now();
        if (!refreshToken.isActive(now)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Refresh token is expired or revoked");
        }

        refreshToken.setRevokedAt(now);
        refreshTokenRepository.save(refreshToken);

        return issueTokens(refreshToken.getUser());
    }

    @Override
    public MessageResponse logout(LogoutRequest request) {
        String hashedToken = HashingUtil.sha256(request.refreshToken());
        refreshTokenRepository.findByTokenHash(hashedToken).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(OffsetDateTime.now());
                refreshTokenRepository.save(token);
            }
        });

        return new MessageResponse("Logged out successfully");
    }

    @Override
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        AppUser user = appUserRepository.findByEmailIgnoreCaseAndDeletedFalse(request.email().trim()).orElse(null);
        if (user == null) {
            return new ForgotPasswordResponse(
                    "If this email exists, a reset link has been generated",
                    null);
        }

        String rawResetToken = UUID.randomUUID() + "." + UUID.randomUUID();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setUser(user);
        resetToken.setTokenHash(HashingUtil.sha256(rawResetToken));
        resetToken.setExpiresAt(OffsetDateTime.now().plusMinutes(PASSWORD_RESET_TOKEN_MINUTES));
        passwordResetTokenRepository.save(resetToken);

        return new ForgotPasswordResponse(
                "If this email exists, a reset link has been generated",
                rawResetToken);
    }

    @Override
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String tokenHash = HashingUtil.sha256(request.token());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID, "Reset token is invalid"));

        OffsetDateTime now = OffsetDateTime.now();
        if (!resetToken.isUsable(now)) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Reset token is expired or used");
        }

        AppUser user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        appUserRepository.save(user);

        resetToken.setUsedAt(now);
        passwordResetTokenRepository.save(resetToken);

        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(user.getId());
        activeTokens.forEach(token -> token.setRevokedAt(now));
        refreshTokenRepository.saveAll(activeTokens);

        return new MessageResponse("Password reset successfully");
    }

    private void validateAccountLock(AppUser user) {
        OffsetDateTime now = OffsetDateTime.now();
        if (user.getLockoutUntil() != null && user.getLockoutUntil().isAfter(now)) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED,
                    "Account is temporarily locked due to failed attempts");
        }

        if (!user.isAccountNonLocked()) {
            user.setAccountNonLocked(true);
            user.setFailedLoginAttempts(0);
            user.setLockoutUntil(null);
            appUserRepository.save(user);
        }
    }

    private void registerFailedAttempt(AppUser user) {
        int newAttemptCount = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(newAttemptCount);

        if (newAttemptCount >= authProperties.getMaxFailedAttempts()) {
            user.setAccountNonLocked(false);
            user.setLockoutUntil(OffsetDateTime.now().plusMinutes(authProperties.getLockDurationMinutes()));
        }

        appUserRepository.save(user);
    }

    private void resetFailedAttempts(AppUser user) {
        user.setFailedLoginAttempts(0);
        user.setAccountNonLocked(true);
        user.setLockoutUntil(null);
        appUserRepository.save(user);
    }

    private AuthResponse issueTokens(AppUser user) {
        OffsetDateTime issuedAt = OffsetDateTime.now();
        String accessToken = jwtTokenService.generateAccessToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRoles().stream().map(role -> role.getName().name()).toList());

        String rawRefreshToken = UUID.randomUUID() + "." + UUID.randomUUID();
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(HashingUtil.sha256(rawRefreshToken));
        refreshToken.setExpiresAt(issuedAt.plusDays(jwtProperties.getRefreshTokenDays()));
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                jwtTokenService.getAccessTokenExpiry(issuedAt),
                toAuthUserResponse(user));
    }

    private AuthUserResponse toAuthUserResponse(AppUser user) {
        Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .map(Enum::name)
                .collect(Collectors.toSet());

        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roles);
    }
}
