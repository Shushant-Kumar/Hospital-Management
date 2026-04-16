package com.shushant.hospital_management.security.config;

import tools.jackson.databind.ObjectMapper;
import com.shushant.hospital_management.common.dto.ApiResponse;
import com.shushant.hospital_management.common.dto.ErrorResponse;
import com.shushant.hospital_management.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 30;
    private static final long WINDOW_MILLIS = 60_000L;

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public AuthRateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (!isRateLimitedEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String key = request.getRemoteAddr() + ":" + request.getRequestURI();
        WindowCounter windowCounter = counters.computeIfAbsent(key, ignored -> new WindowCounter());

        if (!windowCounter.tryAcquire()) {
            ErrorResponse error = new ErrorResponse(
                    ErrorCode.RATE_LIMIT_EXCEEDED.name(),
                    "Too many requests. Please try again shortly",
                    List.of(),
                    OffsetDateTime.now());
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ApiResponse.failure(error));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isRateLimitedEndpoint(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())) {
            return false;
        }

        String path = request.getRequestURI();
        return "/api/v1/auth/login".equals(path)
                || "/api/v1/auth/forgot-password".equals(path)
                || "/api/v1/auth/reset-password".equals(path);
    }

    private static final class WindowCounter {
        private final AtomicInteger requests = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();

        private synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            if (now - windowStart >= WINDOW_MILLIS) {
                windowStart = now;
                requests.set(0);
            }
            return requests.incrementAndGet() <= MAX_REQUESTS_PER_MINUTE;
        }
    }
}
