package com.physio.infrastructure.in.security;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
@Slf4j
public class SecurityConfigurations {

    private final SecurityFilter securityFilter;

    // CORS configurável via variáveis de ambiente
    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:4200}")
    private String corsAllowedOrigins;

    @Value("${CORS_ALLOWED_METHODS:GET,POST,PUT,DELETE,OPTIONS,HEAD}")
    private String corsAllowedMethods;

    @Value("${CORS_ALLOWED_HEADERS:*}")
    private String corsAllowedHeaders;

    @Value("${CORS_ALLOW_CREDENTIALS:false}")
    private boolean corsAllowCredentials;

    @Value("${CORS_MAX_AGE:3600}")
    private long corsMaxAge;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // <--- 1. ADICIONE ISSO AQUI
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(req -> {
                    req.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll();
                    req.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                    req.anyRequest().authenticated();
                })
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    // <--- 2. ADICIONE ESTE BEAN INTEIRO AQUI
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse origins from CSV env var
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        if (origins.isEmpty()) {
            log.warn("Nenhuma origem CORS configurada via CORS_ALLOWED_ORIGINS; bloqueando todas as origens por segurança");
            // Leaving allowedOrigins empty will effectively block cross-origin requests
        } else if (origins.size() == 1 && "*".equals(origins.get(0))) {
            // Note: Allowing all origins and credentials together is not allowed by browsers.
            log.info("CORS configurado para permitir todas as origens ('*')");
            configuration.setAllowedOrigins(Collections.singletonList("*"));
        } else {
            configuration.setAllowedOrigins(origins);
            log.info("CORS allowed origins: {}", origins);
        }

        // Methods
        List<String> methods = Arrays.stream(corsAllowedMethods.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (methods.isEmpty()) methods = Arrays.asList("GET","POST","PUT","DELETE","OPTIONS","HEAD");
        configuration.setAllowedMethods(methods);

        // Headers
        List<String> headers = Arrays.stream(corsAllowedHeaders.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        if (headers.isEmpty()) headers = Collections.singletonList("*");
        configuration.setAllowedHeaders(headers);

        configuration.setAllowCredentials(corsAllowCredentials);
        configuration.setMaxAge(corsMaxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}