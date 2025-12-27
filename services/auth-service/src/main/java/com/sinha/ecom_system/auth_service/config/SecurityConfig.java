package com.sinha.ecom_system.auth_service.config;

import com.sinha.ecom_system.auth_service.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security Configuration
 * 
 * Configures JWT-based stateless authentication
 * - Disables CSRF (not needed for stateless APIs)
 * - Defines public vs protected endpoints
 * - Uses JWT filter for authentication
 * - No server-side sessions
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Configure security filter chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF disabled for stateless JWT API
            .csrf(AbstractHttpConfigurer::disable)
            
            // Define endpoint access rules
            .authorizeHttpRequests(authorize -> authorize
                // Public endpoints - no token required
                .requestMatchers("/auth/register").permitAll()
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/auth/refresh").permitAll()
                .requestMatchers("/auth/forgot-password").permitAll()
                .requestMatchers("/auth/reset-password").permitAll()
                .requestMatchers("/auth/verify-email").permitAll()

                // Protected endpoints - require valid JWT
                .requestMatchers("/auth/logout").authenticated()
                .requestMatchers("/auth/logout-all").authenticated()
                .requestMatchers("/auth/change-password").authenticated()
                .requestMatchers("/auth/profile").authenticated()
                .requestMatchers("/auth/enable-2fa").authenticated()

                // Monitoring endpoints
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/error").permitAll()

                // All other endpoints require authentication
                .anyRequest().authenticated()
            )
            
            // Stateless session - no HttpSession created
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Add custom JWT filter before default authentication filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Password encoder bean using BCrypt
     * Strength 12 = 2^12 hashing iterations
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}

