package com.smartcommunity.attachment;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.smartcommunity.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private final Cloudinary cloudinary;

    @Value("${cloudinary.cloud-name:demo}")
    private String cloudName;

    private static final Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "svg"
    );

    private static final Set<String> VIDEO_EXTENSIONS = Set.of(
            "mp4", "webm", "mov", "mkv", "avi", "m4v"
    );

    private static final Set<String> AUDIO_EXTENSIONS = Set.of(
            "mp3", "wav", "ogg", "m4a"
    );

    private static final Set<String> DOC_EXTENSIONS = Set.of(
            "pdf", "txt", "docx", "doc", "zip", "csv", "xlsx"
    );

    /**
     * Uploads an image, video, audio or document to Cloudinary and returns its secure CDN URL.
     *
     * @param file the uploaded multipart file
     * @param folder directory/folder inside Cloudinary (e.g. "smart-community/messages" or "smart-community/avatars")
     * @return direct secure CDN HTTPS URL
     */
    public String store(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Cannot upload an empty file.");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = getFileExtension(originalFilename).toLowerCase();

        boolean isImage = IMAGE_EXTENSIONS.contains(extension);
        boolean isVideo = VIDEO_EXTENSIONS.contains(extension);
        boolean isAudio = AUDIO_EXTENSIONS.contains(extension);
        boolean isDoc = DOC_EXTENSIONS.contains(extension);

        if (!isImage && !isVideo && !isAudio && !isDoc) {
            throw new BadRequestException("File type ." + extension + " is not permitted.");
        }

        String resourceType = isVideo ? "video" : (isImage ? "image" : "auto");

        try {
            // If using default demo placeholder, provide clean mock URL
            if ("demo".equalsIgnoreCase(cloudName) || !StringUtils.hasText(cloudName)) {
                log.info("Cloudinary demo/local placeholder mode active. Generating mock CDN URL.");
                return "https://res.cloudinary.com/demo/" + (isVideo ? "video" : "image") + "/upload/smart-community/" + UUID.randomUUID() + "." + extension;
            }

            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", resourceType,
                            "public_id", UUID.randomUUID().toString()
                    )
            );

            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("File {} successfully uploaded to Cloudinary: {}", originalFilename, secureUrl);
            return secureUrl;
        } catch (IOException e) {
            log.error("Cloudinary upload failed for file {}: {}", originalFilename, e.getMessage());
            throw new BadRequestException("Failed to upload file to Cloudinary: " + e.getMessage());
        }
    }

    public String store(MultipartFile file) {
        return store(file, "smart-community/attachments");
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1);
    }
}
