package com.smartcommunity.ratelimit;

import com.smartcommunity.common.exception.BadRequestException;
import com.smartcommunity.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RateLimitService rateLimitService;

    @Before("@annotation(withRateLimit)")
    public void checkRateLimit(WithRateLimit withRateLimit) {
        String key = resolveClientKey();
        boolean allowed = rateLimitService.tryConsume(key, withRateLimit.type());

        if (!allowed) {
            log.warn("Rate limit exceeded for key {} under type {}", key, withRateLimit.type());
            throw new BadRequestException("Rate limit exceeded. Please wait a moment and try again.");
        }
    }

    @Before("@annotation(rateLimited)")
    public void checkRateLimited(RateLimited rateLimited) {
        String key = resolveClientKey();
        boolean allowed = rateLimitService.tryConsume(key, rateLimited.type());

        if (!allowed) {
            log.warn("Rate limit exceeded for key {} under type {}", key, rateLimited.type());
            throw new BadRequestException("Rate limit exceeded. Please wait a moment and try again.");
        }
    }

    private String resolveClientKey() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof UserPrincipal principal) {
            return "user:" + principal.getId();
        }

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return "ip:" + xForwardedFor.split(",")[0].trim();
            }
            return "ip:" + request.getRemoteAddr();
        }

        return "unknown";
    }
}
