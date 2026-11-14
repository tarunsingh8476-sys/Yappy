# Yappy — Real-Time Communication Platform with AI Moderation

## 🏗 Technology Stack

- **Backend:** Spring Boot 3.3.4 (Java 21), Spring Security 6 (Stateless JWT), Spring Data JPA, Spring WebSocket (STOMP + SockJS)
- **Database & Persistence:** Pure Hibernate auto-generated tables (`spring.jpa.hibernate.ddl-auto: update`) with embedded local database (persisted in `./data/`).
- **File & Media Storage:** **Cloudinary CDN** (direct uploads for attachments & profile avatars).
- **Email Delivery:** **Brevo** (formerly Sendinblue) SMTP relay for email verification and password recovery, with a local console fallback for offline testing.
- **Rate Limiting:** In-memory Token Bucket via Bucket4j ([`RateLimitService`](file:///C:/Users/Tarun/.gemini/antigravity/scratch/smart-community/src/main/java/com/smartcommunity/ratelimit/RateLimitService.java)).
- **AI/ML Service:** Python FastAPI with multi-class toxicity classifier (`/predict`), plus built-in Java heuristic fallback.
- **API Documentation:** SpringDoc OpenAPI 3 / Postman Documentation

