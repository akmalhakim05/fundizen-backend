package com.fundizen.fundizen_backend.controller;

import com.fundizen.fundizen_backend.service.CloudinaryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
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
            // Validate file before processing
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "No file provided"
                ));
            }

            // Log file details
            logger.info("Uploading campaign image: {} ({})", file.getOriginalFilename(), formatFileSize(file.getSize()));
            
            String url = cloudinaryService.uploadCampaignImage(file);
            
            logger.info("Campaign image uploaded successfully: {}", url);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "url", url,
                "type", "image",
                "fileName", file.getOriginalFilename(),
                "fileSize", formatFileSize(file.getSize()),
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
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "No file provided"
                ));
            }

            logger.info("Uploading campaign document: {} ({})", file.getOriginalFilename(), formatFileSize(file.getSize()));
            
            String url = cloudinaryService.uploadCampaignDocument(file);
            
            logger.info("Campaign document uploaded successfully: {}", url);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "url", url,
                "type", "document",
                "fileName", file.getOriginalFilename(),
                "fileSize", formatFileSize(file.getSize()),
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
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "No file provided"
                ));
            }

            logger.info("Uploading profile image: {} ({})", file.getOriginalFilename(), formatFileSize(file.getSize()));
            
            String url = cloudinaryService.uploadProfileImage(file);
            
            logger.info("Profile image uploaded successfully: {}", url);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "url", url,
                "type", "profile-image",
                "fileName", file.getOriginalFilename(),
                "fileSize", formatFileSize(file.getSize()),
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
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", "No file provided"
                ));
            }

            logger.info("Uploading generic file: {} ({})", file.getOriginalFilename(), formatFileSize(file.getSize()));
            
            String url = cloudinaryService.uploadFile(file);
            
            logger.info("File uploaded successfully: {}", url);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "url", url,
                "fileName", file.getOriginalFilename(),
                "fileSize", formatFileSize(file.getSize()),
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
                logger.info("File deleted successfully: {}", publicId);
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

    /**
     * Handle MaxUploadSizeExceededException
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        logger.warn("File upload size exceeded maximum limit: {}", e.getMessage());
        
        return ResponseEntity.status(413).body(Map.of(
            "success", false,
            "error", "File size too large",
            "message", "The uploaded file exceeds the maximum allowed size of 10MB. Please choose a smaller file.",
            "maxSize", "10MB",
            "errorCode", "FILE_SIZE_EXCEEDED"
        ));
    }

    /**
     * Format file size for display
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int z = (63 - Long.numberOfLeadingZeros(bytes)) / 10;
        return String.format("%.1f %sB", (double) bytes / (1L << (z * 10)), " KMGTPE".charAt(z));
    }
}