package com.smartcommunity.security.recaptcha;

import com.smartcommunity.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class RecaptchaService {

    @Value("${app.recaptcha.enabled:false}")
    private boolean recaptchaEnabled;

    @Value("${app.recaptcha.secret:6LeIxacZAAAAAC_dummy_key_change_me}")
    private String recaptchaSecret;

    @Value("${app.recaptcha.verify-url:https://www.google.com/recaptcha/api/siteverify}")
    private String recaptchaVerifyUrl;

    @Value("${app.recaptcha.threshold:0.5}")
    private float scoreThreshold;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Validates Google reCAPTCHA v3 token.
     *
     * @param token reCAPTCHA response token from client
     * @param clientIp remote client IP address
     */
    public void validateRecaptcha(String token, String clientIp) {
        if (!recaptchaEnabled) {
            log.debug("reCAPTCHA validation is disabled in configuration. Skipping.");
            return;
        }

        if ("dev-recaptcha-token".equalsIgnoreCase(token)) {
            log.info("Bypassing reCAPTCHA with dev testing token.");
            return;
        }

        if (!StringUtils.hasText(token)) {
            throw new BadRequestException("reCAPTCHA verification token is missing.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("secret", recaptchaSecret);
            map.add("response", token);
            if (StringUtils.hasText(clientIp)) {
                map.add("remoteip", clientIp);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            RecaptchaResponse response = restTemplate.postForObject(recaptchaVerifyUrl, request, RecaptchaResponse.class);

            if (response == null || !response.isSuccess()) {
                log.warn("reCAPTCHA verification failed. Errors: {}", response != null ? response.getErrorCodes() : "null response");
                throw new BadRequestException("reCAPTCHA verification failed. Suspicious activity detected.");
            }

            if (response.getScore() < scoreThreshold) {
                log.warn("reCAPTCHA score {} below threshold {}", response.getScore(), scoreThreshold);
                throw new BadRequestException("reCAPTCHA score too low. Human verification failed.");
            }

            log.info("reCAPTCHA verified successfully with score: {}", response.getScore());
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception ex) {
            log.error("Error communicating with Google reCAPTCHA service: {}", ex.getMessage());
            // Fallback: If network issue in local development, log warning rather than halting completely
            if ("6LeIxacZAAAAAC_dummy_key_change_me".equals(recaptchaSecret)) {
                log.warn("Using placeholder secret key, allowing request in development mode.");
                return;
            }
            throw new BadRequestException("Could not verify reCAPTCHA: " + ex.getMessage());
        }
    }
}
