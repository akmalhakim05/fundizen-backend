package com.fundizen.fundizen_backend.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary() {
        // Load environment variables with fallbacks
        String cloudName = getEnvVar("CLOUDINARY_CLOUD_NAME");
        String apiKey = getEnvVar("CLOUDINARY_API_KEY");
        String apiSecret = getEnvVar("CLOUDINARY_API_SECRET");

        // Validate required environment variables
        if (cloudName == null || apiKey == null || apiSecret == null) {
            throw new IllegalStateException(
                "Missing Cloudinary configuration. Please set CLOUDINARY_CLOUD_NAME, " +
                "CLOUDINARY_API_KEY, and CLOUDINARY_API_SECRET environment variables."
            );
        }

        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true // Force HTTPS
        ));
    }

    private String getEnvVar(String key) {
        // Try system environment first
        String value = System.getenv(key);
        if (value != null) {
            return value;
        }

        // Try dotenv file as fallback
        try {
            Dotenv dotenv = Dotenv.load();
            return dotenv.get(key);
        } catch (Exception e) {
            return null;
        }
    }
}