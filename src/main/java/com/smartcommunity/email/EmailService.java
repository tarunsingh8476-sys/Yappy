package com.smartcommunity.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Sends e‑mail via **Brevo Transactional Email API**.
 *
 * <p>
 *   The service reads the Brevo credentials from the environment:
 *
 *   <ul>
 *       <li>{@code brevo.api-key}      – Brevo (SendinBlue) API key</li>
 *       <li>{@code brevo.sender-email} – From address (e.g. {@code noreply@myapp.com})</li>
 *       <li>{@code brevo.sender-name}  – Friendly name shown in the “From” field</li>
 *   </ul>
 *
 *   In a local development environment where {@code brevo.api-key} is **not** set
 *   the class falls back to logging the verification / reset URLs so you can
 *   still test the flow without sending real e‑mail.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String BREVO_ENDPOINT = "https://api.brevo.com/v3/smtp/email";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.frontend.url:")
    private String frontendUrl;

    @Value("${brevo.sender-email:}")
    private String senderEmail;

    @Value("${brevo.sender-name:Smart Community}")
    private String senderName;

    @Value("${brevo.api-key:}")
    private String brevoApiKey;          // empty → dev‑mode fallback

    @Async("taskExecutor")
    public void sendVerificationEmail(@NotBlank String toEmail, @NotBlank String token) {
        String verificationUrl = frontendUrl + "/verify-email?token=" + token;
        String subject = "Smart Community – Verify Your Email";
        String html = buildVerificationHtml(verificationUrl);

        if (!StringUtils.hasText(brevoApiKey)) {
            logInfoFallback("Verification", toEmail, verificationUrl);
            return;
        }

        try {
            BrevoPayload payload = BrevoPayload.builder()
                    .sender(new Sender(senderEmail, senderName))
                    .to(new Recipient(toEmail))
                    .subject(subject)
                    .htmlContent(html)
                    .build();

            postToBrevo(payload);
            log.info("✅ Verification e‑mail sent via Brevo to {}", toEmail);
        } catch (Exception ex) {
            log.error("❌ Failed to send verification e‑mail via Brevo to {} – {}", toEmail, ex.getMessage());
            logInfoFallback("Verification", toEmail, verificationUrl);
        }
    }

    @Async("taskExecutor")
    public void sendPasswordResetEmail(@NotBlank String toEmail, @NotBlank String token) {
        String resetUrl = frontendUrl + "/reset-password?token=" + token;
        String subject = "Smart Community – Password Reset Request";
        String html = buildPasswordResetHtml(resetUrl);

        if (!StringUtils.hasText(brevoApiKey)) {
            logInfoFallback("Password‑reset", toEmail, resetUrl);
            return;
        }

        try {
            BrevoPayload payload = BrevoPayload.builder()
                    .sender(new Sender(senderEmail, senderName))
                    .to(new Recipient(toEmail))
                    .subject(subject)
                    .htmlContent(html)
                    .build();

            postToBrevo(payload);
            log.info("✅ Password‑reset e‑mail sent via Brevo to {}", toEmail);
        } catch (Exception ex) {
            log.error("❌ Failed to send password‑reset e‑mail via Brevo to {} – {}", toEmail, ex.getMessage());
            logInfoFallback("Password‑reset", toEmail, resetUrl);
        }
    }


    private void postToBrevo(BrevoPayload payload) throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(brevoApiKey);

        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
        ResponseEntity<String> response = restTemplate.exchange(
                BREVO_ENDPOINT,
                HttpMethod.POST,
                request,
                String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Brevo responded with status " + response.getStatusCode());
        }
    }

    private String buildVerificationHtml(String verificationUrl) {
        return "<div style='font-family:Arial,Helvetica,sans-serif;max-width:600px;margin:0 auto;padding:20px;border:1px solid #e0e0e0;border-radius:8px;'>"
                + "<h2 style='color:#4f46e5;'>Welcome to Smart Community!</h2>"
                + "<p>Thank you for signing up. Please verify your e‑mail address to activate your account:</p>"
                + "<div style='margin:24px 0;'><a href='" + verificationUrl
                + "' style='background:#4f46e5;color:#fff;padding:12px 24px;text-decoration:none;border-radius:6px;font-weight:bold;'>Verify Email Address</a></div>"
                + "<p style='color:#6b7280;font-size:13px;'>This link expires in 24 hours. If you did not create an account you can safely ignore this e‑mail.</p>"
                + "</div>";
    }

    private String buildPasswordResetHtml(String resetUrl) {
        return "<div style='font-family:Arial,Helvetica,sans-serif;max-width:600px;margin:0 auto;padding:20px;border:1px solid #e0e0e0;border-radius:8px;'>"
                + "<h2 style='color:#dc2626;'>Password Reset Request</h2>"
                + "<p>We received a request to reset your Smart Community password. Click the button below to choose a new password:</p>"
                + "<div style='margin:24px 0;'><a href='" + resetUrl
                + "' style='background:#dc2626;color:#fff;padding:12px 24px;text-decoration:none;border-radius:6px;font-weight:bold;'>Reset Password</a></div>"
                + "<p style='color:#6b7280;font-size:13px;'>This link is valid for 1 hour. If you did not request this, please ignore this e‑mail.</p>"
                + "</div>";
    }

    private void logInfoFallback(String type, String toEmail, String url) {
        log.info("\n================ [BREVO FALLBACK] ================\n"
                        + "To      : {}\n"
                        + "Type    : {}\n"
                        + "Link    : {}\n"
                        + "====================================================",
                toEmail, type, url);
    }


    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    private static class BrevoPayload {
        private Sender sender;
        private Recipient to;
        private String subject;
        private String htmlContent;
    }

    @lombok.Data @lombok.AllArgsConstructor
    private static class Sender {
        private String email;
        private String name;
    }

    @lombok.Data @lombok.AllArgsConstructor
    private static class Recipient {
        private String email;
    }
}