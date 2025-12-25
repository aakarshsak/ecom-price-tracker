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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF (for stateless JWT-based API)
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configure endpoint authorization
            .authorizeHttpRequests(authorize -> authorize
                // Public endpoints (no authentication required)
                .requestMatchers("/auth/register").permitAll()
                .requestMatchers("/auth/login").permitAll()
                .requestMatchers("/auth/refresh").permitAll()
                .requestMatchers("/auth/forgot-password").permitAll()
                .requestMatchers("/auth/reset-password").permitAll()
                .requestMatchers("/auth/verify-email").permitAll()

                // Protected auth endpoints (require authentication)
                .requestMatchers("/auth/logout").authenticated()
                .requestMatchers("/auth/logout-all").authenticated()
                .requestMatchers("/auth/change-password").authenticated()
                .requestMatchers("/auth/profile").authenticated()
                .requestMatchers("/auth/enable-2fa").authenticated()

                // Health check
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/error").permitAll()

                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            
            // Stateless session (JWT-based, no server-side sessions)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Add JWT authentication filter before Spring Security's default filter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // Strength 12 for password hashing
    }
}

