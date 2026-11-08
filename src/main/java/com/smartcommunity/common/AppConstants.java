package com.smartcommunity.common;

public final class AppConstants {

    private AppConstants() {}

    public static final String SERVER_ROLE_OWNER = "OWNER";
    public static final String SERVER_ROLE_MODERATOR = "MODERATOR";
    public static final String SERVER_ROLE_MEMBER = "MEMBER";

    public static final String CHANNEL_TYPE_TEXT = "TEXT";
    public static final String CHANNEL_TYPE_VOICE = "VOICE";

    public static final String VISIBILITY_PUBLIC = "PUBLIC";
    public static final String VISIBILITY_PRIVATE = "PRIVATE";

    public static final String MESSAGE_STATUS_PUBLISHED = "PUBLISHED";
    public static final String MESSAGE_STATUS_HELD_FOR_REVIEW = "HELD_FOR_REVIEW";
    public static final String MESSAGE_STATUS_REMOVED = "REMOVED";

    public static final String MOD_STATUS_PENDING = "PENDING";
    public static final String MOD_STATUS_DISMISSED = "DISMISSED";
    public static final String MOD_STATUS_WARNED = "WARNED";
    public static final String MOD_STATUS_REMOVED = "REMOVED";
    public static final String MOD_STATUS_MUTED = "MUTED";
    public static final String MOD_STATUS_SUSPENDED = "SUSPENDED";
}
