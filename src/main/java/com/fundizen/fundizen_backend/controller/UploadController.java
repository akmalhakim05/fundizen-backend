package com.fundizen.fundizen_backend.controller;

import com.fundizen.fundizen_backend.service.CloudinaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
public class UploadController {

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);
    private final CloudinaryService cloudinaryService;

    public UploadController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    /**
     * Upload campaign image
     */
    @PostMapping("/campaign/image")
    public ResponseEntity<?> uploadCampaignImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = cloudinaryService.uploadCampaignImage(file);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "url", url,
                "type", "image",
                "message", "Campaign image uploaded successfully"
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid file upload attempt: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error uploading campaign image", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to upload image: " + e.getMessage()
            ));
        }
    }

    /**
     * Upload campaign document
     */
    @PostMapping("/campaign/document")
    public ResponseEntity<?> uploadCampaignDocument(@RequestParam("file") MultipartFile file) {
        try {
            String url = cloudinaryService.uploadCampaignDocument(file);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "url", url,
                "type", "document",
                "message", "Campaign document uploaded successfully"
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid document upload attempt: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error uploading campaign document", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to upload document: " + e.getMessage()
            ));
        }
    }

    /**
     * Upload profile image
     */
    @PostMapping("/profile/image")
    public ResponseEntity<?> uploadProfileImage(@RequestParam("file") MultipartFile file) {
        try {
            String url = cloudinaryService.uploadProfileImage(file);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "url", url,
                "type", "profile-image",
                "message", "Profile image uploaded successfully"
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid profile image upload attempt: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error uploading profile image", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to upload profile image: " + e.getMessage()
            ));
        }
    }

    /**
     * Generic file upload (backward compatibility)
     */
    @PostMapping
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String url = cloudinaryService.uploadFile(file);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "url", url,
                "message", "File uploaded successfully"
            ));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid file upload attempt: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        } catch (Exception e) {
            logger.error("Error uploading file", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to upload file: " + e.getMessage()
            ));
        }
    }

    /**
     * Delete uploaded file
     */
    @DeleteMapping
    public ResponseEntity<?> deleteFile(@RequestParam("url") String cloudinaryUrl) {
        try {
            String publicId = cloudinaryService.extractPublicId(cloudinaryUrl);
            if (publicId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "Invalid Cloudinary URL"
                ));
            }

            boolean deleted = cloudinaryService.deleteFile(publicId);
            
            if (deleted) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "File deleted successfully"
                ));
            } else {
                return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Failed to delete file"
                ));
            }
        } catch (Exception e) {
            logger.error("Error deleting file", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to delete file: " + e.getMessage()
            ));
        }
    }

    /**
     * Get optimized image URL
     */
    @GetMapping("/optimize")
    public ResponseEntity<?> getOptimizedImageUrl(
            @RequestParam("url") String originalUrl,
            @RequestParam(defaultValue = "800") int width,
            @RequestParam(defaultValue = "600") int height,
            @RequestParam(defaultValue = "fill") String crop) {
        try {
            String optimizedUrl = cloudinaryService.getOptimizedImageUrl(originalUrl, width, height, crop);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "originalUrl", originalUrl,
                "optimizedUrl", optimizedUrl,
                "transformations", Map.of(
                    "width", width,
                    "height", height,
                    "crop", crop
                )
            ));
        } catch (Exception e) {
            logger.error("Error generating optimized URL", e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to generate optimized URL: " + e.getMessage()
            ));
        }
    }
}