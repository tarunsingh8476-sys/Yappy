package com.smartcommunity.user;

import com.smartcommunity.attachment.StorageService;
import com.smartcommunity.common.exception.ResourceNotFoundException;
import com.smartcommunity.user.dto.UpdateProfileRequest;
import com.smartcommunity.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = findUserById(userId);
        return UserProfileResponse.from(user);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserById(userId);

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio().trim());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus().trim());
        }

        User updatedUser = userRepository.save(user);
        log.info("Profile updated for user: {}", user.getEmail());
        return UserProfileResponse.from(updatedUser);
    }

    @Transactional
    public UserProfileResponse uploadAvatar(Long userId, MultipartFile file) {
        User user = findUserById(userId);
        String avatarUrl = storageService.store(file, "smart-community/avatars");
        user.setAvatarUrl(avatarUrl);
        User updated = userRepository.save(user);
        log.info("Avatar updated via Cloudinary for user: {}", user.getEmail());
        return UserProfileResponse.from(updated);
    }

    @Transactional
    public void updateUserStatus(Long userId, String status) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(status);
            userRepository.save(user);
        });
    }

    @Transactional(readOnly = true)
    public User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
