package com.smartcommunity.auth;

import com.smartcommunity.auth.dto.*;
import com.smartcommunity.common.ApiResponse;
import com.smartcommunity.ratelimit.RateLimitType;
import com.smartcommunity.ratelimit.WithRateLimit;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, JWT login, OAuth2 (Google/GitHub), reCAPTCHA, and password reset APIs")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @WithRateLimit(type = RateLimitType.AUTH)
    @Operation(summary = "Register new account", description = "Creates a new user profile, enforces reCAPTCHA v3 verification, sends verification email, and returns JWT tokens.")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        AuthResponse response = authService.register(request, clientIp);
        return new ResponseEntity<>(ApiResponse.success(response, "Account registered successfully."), HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @WithRateLimit(type = RateLimitType.AUTH)
    @Operation(summary = "User login", description = "Authenticates user with email/username and password, verifies reCAPTCHA, and issues JWT access and refresh tokens.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        AuthResponse response = authService.login(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful."));
    }

    @PostMapping("/oauth/{provider}")
    @Operation(summary = "OAuth2 Social Login (Google / GitHub)", description = "Authenticates or registers users with Google or GitHub OAuth identity provider.")
    public ResponseEntity<ApiResponse<AuthResponse>> oauthLogin(
            @PathVariable String provider,
            @Valid @RequestBody OAuthLoginRequest request) {
        request.setProvider(provider);
        AuthResponse response = authService.oauthLogin(request);
        return ResponseEntity.ok(ApiResponse.success(response, "OAuth login successful."));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT Access Token", description = "Issues a fresh JWT access token using a valid, unexpired refresh token.")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully."));
    }

    @GetMapping("/verify-email")
    @Operation(summary = "Verify user email address", description = "Verifies email address using the token delivered via Brevo SMTP email.")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success("Email verified successfully. You may now access all community features."));
    }

    @PostMapping("/forgot-password")
    @WithRateLimit(type = RateLimitType.AUTH)
    @Operation(summary = "Request password reset", description = "Initiates password reset and sends token via Brevo SMTP email.")
    public ResponseEntity<ApiResponse<String>> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        authService.requestPasswordReset(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("If an account exists with that email, a password reset link has been dispatched."));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Confirm password reset", description = "Resets user password with a valid, unexpired password reset token.")
    public ResponseEntity<ApiResponse<String>> confirmPasswordReset(
            @Valid @RequestBody ConfirmPasswordResetRequest request) {
        authService.confirmPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success("Your password has been reset successfully. Please log in with your new password."));
    }
}
