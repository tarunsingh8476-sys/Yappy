package com.smartcommunity.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class RateLimitService {

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.rate-limit.general.capacity:100}")
    private long generalCapacity;
    @Value("${app.rate-limit.general.refill-tokens:100}")
    private long generalRefillTokens;
    @Value("${app.rate-limit.general.refill-period-seconds:60}")
    private long generalPeriod;

    @Value("${app.rate-limit.auth.capacity:10}")
    private long authCapacity;
    @Value("${app.rate-limit.auth.refill-tokens:10}")
    private long authRefillTokens;
    @Value("${app.rate-limit.auth.refill-period-seconds:60}")
    private long authPeriod;

    @Value("${app.rate-limit.messaging.capacity:30}")
    private long messagingCapacity;
    @Value("${app.rate-limit.messaging.refill-tokens:30}")
    private long messagingRefillTokens;
    @Value("${app.rate-limit.messaging.refill-period-seconds:60}")
    private long messagingPeriod;

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String key, RateLimitType type) {
        if (!enabled) {
            return true;
        }

        String bucketKey = type.name() + ":" + key;
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createNewBucket(type));
        return bucket.tryConsume(1);
    }

    private Bucket createNewBucket(RateLimitType type) {
        long capacity;
        long refillTokens;
        long periodSeconds;

        switch (type) {
            case AUTH -> {
                capacity = authCapacity;
                refillTokens = authRefillTokens;
                periodSeconds = authPeriod;
            }
            case MESSAGING -> {
                capacity = messagingCapacity;
                refillTokens = messagingRefillTokens;
                periodSeconds = messagingPeriod;
            }
            case GENERAL -> {
                capacity = generalCapacity;
                refillTokens = generalRefillTokens;
                periodSeconds = generalPeriod;
            }
            default -> {
                capacity = 100;
                refillTokens = 100;
                periodSeconds = 60;
            }
        }

        Bandwidth limit = Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(refillTokens, Duration.ofSeconds(periodSeconds))
                .build();

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
