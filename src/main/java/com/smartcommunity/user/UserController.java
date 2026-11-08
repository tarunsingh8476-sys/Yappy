package com.smartcommunity.user;

import com.smartcommunity.ratelimit.RateLimitType;
import com.smartcommunity.ratelimit.RateLimited;
import com.smartcommunity.security.CurrentUser;
import com.smartcommunity.security.UserPrincipal;
import com.smartcommunity.user.dto.UpdateProfileRequest;
import com.smartcommunity.user.dto.UserProfileResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Endpoints for user profiles and avatar management")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get currently authenticated user's profile")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@CurrentUser UserPrincipal currentUser) {
        return ResponseEntity.ok(userService.getUserProfile(currentUser.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user profile by ID")
    public ResponseEntity<UserProfileResponse> getUserById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(userService.getUserProfile(id));
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "Get user profile by username handle")
    public ResponseEntity<UserProfileResponse> getUserByUsername(@PathVariable("username") String username) {
        return ResponseEntity.ok(userService.getUserByUsername(username));
    }

    @PatchMapping("/me")
    @Operation(summary = "Update current user profile (display name, bio, status)")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @CurrentUser UserPrincipal currentUser,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(currentUser.getId(), request));
    }

    @PostMapping("/me/avatar")
    @RateLimited(type = RateLimitType.USER, capacity = 10, refillTokens = 10, durationSeconds = 60)
    @Operation(summary = "Upload user avatar directly to Cloudinary CDN")
    public ResponseEntity<UserProfileResponse> uploadAvatar(
            @CurrentUser UserPrincipal currentUser,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(userService.uploadAvatar(currentUser.getId(), file));
    }
}
