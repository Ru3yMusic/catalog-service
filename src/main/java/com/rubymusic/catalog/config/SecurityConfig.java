package com.rubymusic.catalog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless REST security configuration for catalog-service.
 *
 * <p>Authorization strategy:
 * <ul>
 *   <li>{@code /api/internal/v1/**} — requires {@code ROLE_SERVICE} JWT</li>
 *   <li>{@code GET /api/v1/catalog/**} — public (browsing catalog requires no auth)</li>
 *   <li>{@code /actuator/**, /swagger-ui/**, /v3/api-docs/**} — public infrastructure</li>
 *   <li>Any other request — requires valid JWT (USER or SERVICE)</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> res.sendError(401, "Unauthorized"))
                        .accessDeniedHandler((req, res, e) -> res.sendError(403, "Forbidden"))
                )
                .authorizeHttpRequests(auth -> auth
                        // Internal service-to-service endpoints require SERVICE role
                        .requestMatchers("/api/internal/v1/**").hasAuthority("ROLE_SERVICE")

                        // Public infrastructure
                        .requestMatchers("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // Catalog read endpoints are publicly accessible (no JWT required)
                        .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**").permitAll()

                        // Write operations (POST/PUT/DELETE) and any other path require authentication
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
