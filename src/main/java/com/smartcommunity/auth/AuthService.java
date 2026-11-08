package com.smartcommunity.auth;

import com.smartcommunity.auth.dto.*;
import com.smartcommunity.common.exception.BadRequestException;
import com.smartcommunity.common.exception.UnauthorizedException;
import com.smartcommunity.email.EmailService;
import com.smartcommunity.security.JwtTokenProvider;
import com.smartcommunity.security.UserPrincipal;
import com.smartcommunity.security.recaptcha.RecaptchaService;
import com.smartcommunity.user.User;
import com.smartcommunity.user.UserRepository;
import com.smartcommunity.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final EmailService emailService;
    private final RecaptchaService recaptchaService;

    @Value("${app.jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @Transactional
    public AuthResponse register(RegisterRequest request, String clientIp) {
        recaptchaService.validateRecaptcha(request.getRecaptchaToken(), clientIp);

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email address is already registered.");
        }

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .username(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .displayName(StringUtils.hasText(request.getDisplayName()) ? request.getDisplayName().trim() : request.getUsername().trim())
                .emailVerificationToken(verificationToken)
                .emailVerified(false)
                .status("ONLINE")
                .build();

        User savedUser = userRepository.save(user);

        // Send verification email
        emailService.sendVerificationEmail(savedUser.getEmail(), verificationToken);

        UserPrincipal userPrincipal = UserPrincipal.create(savedUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());

        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresInMs(jwtExpirationMs)
                .user(UserProfileResponse.from(savedUser))
                .build();
    }

    public AuthResponse login(LoginRequest request, String clientIp) {
        recaptchaService.validateRecaptcha(request.getRecaptchaToken(), clientIp);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail().trim(), request.getPassword())
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (user.isSuspended()) {
            throw new BadRequestException("Your account has been suspended by a community administrator.");
        }

        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresInMs(jwtExpirationMs)
                .user(UserProfileResponse.from(user))
                .build();
    }

    @Transactional
    public AuthResponse oauthLogin(OAuthLoginRequest request) {
        String email = request.getEmail();
        if (!StringUtils.hasText(email)) {
            throw new BadRequestException("Email not provided by OAuth identity provider: " + request.getProvider());
        }

        User user = userRepository.findByEmail(email.toLowerCase()).orElseGet(() -> {
            String username = StringUtils.hasText(request.getName()) ?
                    request.getName().toLowerCase().replaceAll("\\s+", "_") + "_" + UUID.randomUUID().toString().substring(0, 4) :
                    "user_" + UUID.randomUUID().toString().substring(0, 8);

            User newUser = User.builder()
                    .email(email.toLowerCase())
                    .username(username)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString())) // secure random placeholder
                    .displayName(request.getName() != null ? request.getName() : username)
                    .avatarUrl(request.getAvatarUrl())
                    .emailVerified(true)
                    .status("ONLINE")
                    .build();

            return userRepository.save(newUser);
        });

        if (user.isSuspended()) {
            throw new BadRequestException("Your account has been suspended.");
        }

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());

        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(authentication);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresInMs(jwtExpirationMs)
                .user(UserProfileResponse.from(user))
                .build();
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (!tokenProvider.validateToken(token)) {
            throw new UnauthorizedException("Invalid or expired refresh token.");
        }

        Long userId = tokenProvider.getUserIdFromJWT(token);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (user.isSuspended()) {
            throw new BadRequestException("Your account is suspended.");
        }

        UserPrincipal userPrincipal = UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());

        String newAccessToken = tokenProvider.generateToken(authentication);
        String newRefreshToken = tokenProvider.generateRefreshToken(authentication);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresInMs(jwtExpirationMs)
                .user(UserProfileResponse.from(user))
                .build();
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token."));

        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        userRepository.save(user);
        log.info("User {} successfully verified email.", user.getUsername());
    }

    @Transactional
    public void requestPasswordReset(PasswordResetRequest request, String clientIp) {
        recaptchaService.validateRecaptcha(request.getRecaptchaToken(), clientIp);

        userRepository.findByEmail(request.getEmail().trim().toLowerCase()).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetTokenExpiry(Instant.now().plus(1, ChronoUnit.HOURS));
            userRepository.save(user);

            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
            log.info("Password reset initiated for user: {}", user.getEmail());
        });
    }

    @Transactional
    public void confirmPasswordReset(ConfirmPasswordResetRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new BadRequestException("Invalid password reset token."));

        if (user.getPasswordResetTokenExpiry() == null || user.getPasswordResetTokenExpiry().isBefore(Instant.now())) {
            throw new BadRequestException("Password reset token has expired.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
        log.info("Password successfully reset for user: {}", user.getUsername());
    }
}
