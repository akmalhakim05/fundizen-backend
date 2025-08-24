package com.fundizen.fundizen_backend.config;

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;

@Configuration
public class FirebaseConfig {

    private static final Logger LOGGER = Logger.getLogger(FirebaseConfig.class.getName());

    @PostConstruct
    public void init() {
        try {
            InputStream serviceAccount = getClass().getResourceAsStream("/serviceAccountKey.json");

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                LOGGER.info("Firebase initialized successfully.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize FirebaseApp", e);
        }
    }
}
