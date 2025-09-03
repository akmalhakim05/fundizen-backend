package com.fundizen.fundizen_backend.config;

import com.fundizen.fundizen_backend.filter.FirebaseAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private FirebaseAuthenticationFilter firebaseAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - allow all authentication methods
                .requestMatchers("/api/users/register", "/api/users/login").permitAll()
                .requestMatchers("/api/auth/**").permitAll() // Allow all auth endpoints
                .requestMatchers("/api/campaigns").permitAll() // GET campaigns (public)
                .requestMatchers("/api/campaigns/{id}").permitAll() // GET single campaign (public)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // Protected endpoints - require authentication
                .requestMatchers("/api/campaigns/create", "/api/campaigns/*/update", "/api/campaigns/*/delete").authenticated()
                .requestMatchers("/api/campaigns/verify/**", "/api/campaigns/reject/**").hasRole("ADMIN")
                .requestMatchers("/api/users/*/promote", "/api/users/*/demote").hasRole("ADMIN")
                .requestMatchers("/api/users/**").authenticated()
                
                .anyRequest().authenticated()
            )
            .addFilterBefore(firebaseAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}