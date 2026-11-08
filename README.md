# Smart Community — Real-Time Communication Platform with AI Moderation

[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.111-teal.svg)](https://fastapi.tiangolo.com/)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A lightweight, production-ready community communication platform inspired by modern team chat applications (e.g. Discord, Slack) with built-in **AI-powered content moderation**, **in-memory token-bucket rate limiting**, **real-time WebSocket/STOMP messaging**, **Cloudinary media storage**, and **Brevo transactional emails**.

---

## 🏗 Technology Stack

- **Backend:** Spring Boot 3.3.4 (Java 21), Spring Security 6 (Stateless JWT), Spring Data JPA, Spring WebSocket (STOMP + SockJS)
- **Database & Persistence:** Pure Hibernate auto-generated tables (`spring.jpa.hibernate.ddl-auto: update`) with embedded local database (persisted in `./data/`).
- **File & Media Storage:** **Cloudinary CDN** (direct uploads for attachments & profile avatars).
- **Email Delivery:** **Brevo** (formerly Sendinblue) SMTP relay for email verification and password recovery, with a local console fallback for offline testing.
- **Rate Limiting:** In-memory Token Bucket via Bucket4j ([`RateLimitService`](file:///C:/Users/Tarun/.gemini/antigravity/scratch/smart-community/src/main/java/com/smartcommunity/ratelimit/RateLimitService.java)).
- **AI/ML Service:** Python FastAPI microservice with multi-class toxicity classifier (`/predict`), plus built-in Java heuristic fallback.
- **API Documentation:** SpringDoc OpenAPI 3 / Swagger UI (`/swagger-ui.html`).

---

## ⚡ Quick Start

```bash
mvn clean spring-boot:run
```

- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **H2 Database Console:** `http://localhost:8080/h2-console`
- **Health Check:** `http://localhost:8080/actuator/health`

---

## ⚙ Environment Configuration

Create a `.env` file (or set environment variables) based on `.env.example`:

```bash
# Cloudinary
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# Brevo (formerly Sendinblue)
BREVO_SMTP_USER=your_brevo_account_login_email
BREVO_SMTP_KEY=xsmtpsib-your-smtp-master-key
BREVO_SENDER_EMAIL=your_verified_sender@domain.com
```

*Note: If Brevo or Cloudinary keys are omitted, the application automatically switches to **offline demo mode** (logs tokens to console and generates mock CDN URLs) so you can test everything immediately without third-party accounts.*
