package com.shushant.hospital_management.security.jwt;

import com.shushant.hospital_management.common.exception.BusinessException;
import com.shushant.hospital_management.common.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService {

    private static final String CLAIM_ROLES = "roles";

    private final JwtProperties jwtProperties;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(String userId, String email, List<String> roles) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime expiresAt = now.plusMinutes(jwtProperties.getAccessTokenMinutes());

        return Jwts.builder()
                .subject(userId)
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(now.toInstant()))
                .expiration(Date.from(expiresAt.toInstant()))
                .claim("email", email)
                .claim(CLAIM_ROLES, roles)
                .signWith(signingKey())
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser().verifyWith(signingKey()).build().parseSignedClaims(token).getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, "Invalid or expired JWT token");
        }
    }

    public String extractUserId(String token) {
        return parseToken(token).getSubject();
    }

    public OffsetDateTime getAccessTokenExpiry(OffsetDateTime issuedAt) {
        return issuedAt.plusMinutes(jwtProperties.getAccessTokenMinutes());
    }

    private SecretKey signingKey() {
        String secret = jwtProperties.getSecret();
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException ignored) {
            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }
}
