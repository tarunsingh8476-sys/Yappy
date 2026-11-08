package com.smartcommunity.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
    RateLimitType type() default RateLimitType.GENERAL;
    long capacity() default 100;
    long refillTokens() default 100;
    long durationSeconds() default 60;
}
