package com.smartcommunity.attachment;

import com.smartcommunity.attachment.dto.UploadResponse;
import com.smartcommunity.common.ApiResponse;
import com.smartcommunity.ratelimit.RateLimitType;
import com.smartcommunity.ratelimit.RateLimited;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachments & Files", description = "Endpoints for uploading images, videos, audio, and documents to Cloudinary CDN")
@SecurityRequirement(name = "BearerAuth")
public class AttachmentController {

    private final StorageService storageService;

    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov", "mkv", "avi", "m4v");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "svg");
    private static final Set<String> AUDIO_EXTENSIONS = Set.of("mp3", "wav", "ogg", "m4a");

    @PostMapping("/upload")
    @RateLimited(type = RateLimitType.GENERAL)
    @Operation(summary = "Upload media or document to Cloudinary CDN",
            description = "Supports image formats (jpg, png, webp, gif), videos (mp4, webm, mov), audio (mp3, wav), and documents (pdf, docx, txt, zip).")
    public ResponseEntity<ApiResponse<UploadResponse>> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "smart-community/attachments") String folder) {

        String secureUrl = storageService.store(file, folder);
        String filename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        String type = "document";
        if (IMAGE_EXTENSIONS.contains(ext)) {
            type = "image";
        } else if (VIDEO_EXTENSIONS.contains(ext)) {
            type = "video";
        } else if (AUDIO_EXTENSIONS.contains(ext)) {
            type = "audio";
        }

        UploadResponse response = UploadResponse.builder()
                .url(secureUrl)
                .originalName(filename)
                .resourceType(type)
                .size(file.getSize())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "File uploaded successfully to Cloudinary."));
    }
}
