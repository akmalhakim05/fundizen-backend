package com.fundizen.fundizen_backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.Arrays;
import java.util.List;

@Service
public class CloudinaryService {

    private static final Logger logger = LoggerFactory.getLogger(CloudinaryService.class);
    private final Cloudinary cloudinary;

    // File type constants
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    
    private static final List<String> ALLOWED_DOCUMENT_TYPES = Arrays.asList(
            "application/pdf", "application/msword", 
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    // File size limits (in bytes)
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_DOCUMENT_SIZE = 10 * 1024 * 1024; // 10MB

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Upload campaign image - simplified version without transformations
     */
    public String uploadCampaignImage(MultipartFile file) throws IOException {
        logger.info("Starting campaign image upload: {} ({})", 
                   file.getOriginalFilename(), formatFileSize(file.getSize()));
        
        validateImageFile(file);
        
        String publicId = "campaigns/" + UUID.randomUUID().toString();
        
        try {
            logger.info("Uploading to Cloudinary with publicId: {}", publicId);
            
            // Simplified upload without transformations first
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "folder", "fundizen/campaigns",
                            "resource_type", "image",
                            "tags", Arrays.asList("campaign", "image"),
                            "overwrite", false
                    ));
            
            String secureUrl = (String) uploadResult.get("secure_url");
            logger.info("Campaign image uploaded successfully: {} -> {}", 
                       file.getOriginalFilename(), secureUrl);
            
            logger.debug("Upload result: {}", uploadResult);
            
            return secureUrl;
            
        } catch (Exception e) {
            logger.error("Cloudinary upload failed for file: {} - Error: {}", 
                        file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Upload campaign document
     */
    public String uploadCampaignDocument(MultipartFile file) throws IOException {
        logger.info("Starting campaign document upload: {} ({})", 
                   file.getOriginalFilename(), formatFileSize(file.getSize()));
        
        validateDocumentFile(file);
        
        String publicId = "documents/" + UUID.randomUUID().toString();
        
        try {
            logger.info("Uploading document to Cloudinary with publicId: {}", publicId);
            
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "folder", "fundizen/documents",
                            "resource_type", "raw",
                            "tags", Arrays.asList("campaign", "document"),
                            "overwrite", false
                    ));
            
            String secureUrl = (String) uploadResult.get("secure_url");
            logger.info("Campaign document uploaded successfully: {} -> {}", 
                       file.getOriginalFilename(), secureUrl);
            return secureUrl;
            
        } catch (Exception e) {
            logger.error("Cloudinary document upload failed for file: {} - Error: {}", 
                        file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Upload user profile image
     */
    public String uploadProfileImage(MultipartFile file) throws IOException {
        logger.info("Starting profile image upload: {} ({})", 
                   file.getOriginalFilename(), formatFileSize(file.getSize()));
        
        validateImageFile(file);
        
        String publicId = "profiles/" + UUID.randomUUID().toString();
        
        try {
            logger.info("Uploading profile image to Cloudinary with publicId: {}", publicId);
            
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "folder", "fundizen/profiles",
                            "resource_type", "image",
                            "tags", Arrays.asList("profile", "image"),
                            "overwrite", false
                    ));
            
            String secureUrl = (String) uploadResult.get("secure_url");
            logger.info("Profile image uploaded successfully: {} -> {}", 
                       file.getOriginalFilename(), secureUrl);
            return secureUrl;
            
        } catch (Exception e) {
            logger.error("Cloudinary profile image upload failed for file: {} - Error: {}", 
                        file.getOriginalFilename(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Generic file upload (backward compatibility)
     */
    public String uploadFile(MultipartFile file) throws IOException {
        logger.info("Starting generic file upload: {} ({})", 
                   file.getOriginalFilename(), formatFileSize(file.getSize()));
        
        if (isImageFile(file)) {
            return uploadCampaignImage(file);
        } else if (isDocumentFile(file)) {
            return uploadCampaignDocument(file);
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + file.getContentType() + 
                                             ". Allowed types: " + ALLOWED_IMAGE_TYPES + ", " + ALLOWED_DOCUMENT_TYPES);
        }
    }

    /**
     * Delete file from Cloudinary
     */
    public boolean deleteFile(String publicId) {
        try {
            logger.info("Deleting file from Cloudinary: {}", publicId);
            
            Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String status = (String) result.get("result");
            boolean success = "ok".equals(status);
            
            if (success) {
                logger.info("File deleted successfully: {}", publicId);
            } else {
                logger.warn("Failed to delete file: {} - Status: {}", publicId, status);
            }
            
            return success;
        } catch (IOException e) {
            logger.error("Error deleting file from Cloudinary: {}", publicId, e);
            return false;
        }
    }

    /**
     * Extract public ID from Cloudinary URL
     */
    public String extractPublicId(String cloudinaryUrl) {
        if (cloudinaryUrl == null || !cloudinaryUrl.contains("cloudinary.com")) {
            return null;
        }
        
        try {
            // Extract public ID from URL
            // Format: https://res.cloudinary.com/cloud_name/image/upload/v1234567890/folder/file.jpg
            String[] parts = cloudinaryUrl.split("/");
            if (parts.length >= 7) {
                // Find the part after 'upload' and before the version (v1234567890)
                int uploadIndex = -1;
                for (int i = 0; i < parts.length; i++) {
                    if ("upload".equals(parts[i])) {
                        uploadIndex = i;
                        break;
                    }
                }
                
                if (uploadIndex != -1 && uploadIndex + 2 < parts.length) {
                    // Skip version if present
                    int startIndex = uploadIndex + 1;
                    if (parts[startIndex].startsWith("v") && parts[startIndex].matches("v\\d+")) {
                        startIndex++;
                    }
                    
                    // Join remaining parts and remove file extension
                    StringBuilder publicId = new StringBuilder();
                    for (int i = startIndex; i < parts.length; i++) {
                        if (i > startIndex) publicId.append("/");
                        publicId.append(parts[i]);
                    }
                    
                    // Remove file extension
                    String result = publicId.toString();
                    int dotIndex = result.lastIndexOf(".");
                    if (dotIndex > 0) {
                        result = result.substring(0, dotIndex);
                    }
                    
                    return result;
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting public ID from URL: {}", cloudinaryUrl, e);
        }
        
        return null;
    }

    /**
     * Validate image file
     */
    private void validateImageFile(MultipartFile file) throws IOException {
        logger.debug("Validating image file: {} (size: {}, type: {})", 
                    file.getOriginalFilename(), formatFileSize(file.getSize()), file.getContentType());
        
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException(
                "Invalid image type: " + file.getContentType() + 
                ". Allowed types: " + ALLOWED_IMAGE_TYPES
            );
        }
        
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException(
                "Image size too large: " + formatFileSize(file.getSize()) + 
                ". Maximum size: " + formatFileSize(MAX_IMAGE_SIZE)
            );
        }
        
        logger.debug("Image file validation passed");
    }

    /**
     * Validate document file
     */
    private void validateDocumentFile(MultipartFile file) throws IOException {
        logger.debug("Validating document file: {} (size: {}, type: {})", 
                    file.getOriginalFilename(), formatFileSize(file.getSize()), file.getContentType());
        
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
        
        if (!ALLOWED_DOCUMENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException(
                "Invalid document type: " + file.getContentType() + 
                ". Allowed types: " + ALLOWED_DOCUMENT_TYPES
            );
        }
        
        if (file.getSize() > MAX_DOCUMENT_SIZE) {
            throw new IllegalArgumentException(
                "Document size too large: " + formatFileSize(file.getSize()) + 
                ". Maximum size: " + formatFileSize(MAX_DOCUMENT_SIZE)
            );
        }
        
        logger.debug("Document file validation passed");
    }

    /**
     * Check if file is an image
     */
    private boolean isImageFile(MultipartFile file) {
        return ALLOWED_IMAGE_TYPES.contains(file.getContentType());
    }

    /**
     * Check if file is a document
     */
    private boolean isDocumentFile(MultipartFile file) {
        return ALLOWED_DOCUMENT_TYPES.contains(file.getContentType());
    }

    /**
     * Get optimized image URL with transformations
     */
    public String getOptimizedImageUrl(String originalUrl, int width, int height, String crop) {
        String publicId = extractPublicId(originalUrl);
        if (publicId == null) {
            return originalUrl; // Return original if can't extract public ID
        }
        
        try {
            return cloudinary.url()
                    .transformation(new Transformation()
                            .width(width)
                            .height(height)
                            .crop(crop != null ? crop : "fill")
                            .quality("auto:good")
                            .fetchFormat("auto"))
                    .generate(publicId);
        } catch (Exception e) {
            logger.error("Error generating optimized URL for: {}", originalUrl, e);
            return originalUrl;
        }
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