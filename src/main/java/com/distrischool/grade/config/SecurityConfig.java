package com.distrischool.grade.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${AUTH0_DOMAIN:}")
    private String auth0Domain;

    @Value("${auth0.audience:}")
    private String audience;

    @Value("${security.disable:false}")
    private boolean securityDisable;
    
    private String getIssuerUri() {
        if (auth0Domain == null || auth0Domain.isEmpty()) {
            return "https://dev-lthr3fyfn4x47q1g.us.auth0.com/";
        }
        // Se já contém https://, usar como está, senão adicionar
        if (auth0Domain.startsWith("https://")) {
            return auth0Domain.endsWith("/") ? auth0Domain : auth0Domain + "/";
        }
        return "https://" + auth0Domain + "/";
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (securityDisable) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        } else {
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/v1/health/**").permitAll()
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );
        }

        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "security.disable", havingValue = "false", matchIfMissing = true)
    public JwtDecoder jwtDecoder() {
        String issuerUri = getIssuerUri();
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuerUri);

        OAuth2TokenValidator<Jwt> withIssuer = org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> withAudience = new AudienceValidator(audience);
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience);

        jwtDecoder.setJwtValidator(validator);
        return jwtDecoder;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
        @Value("${spring.web.cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:8080}") String allowedOrigins,
        @Value("${spring.web.cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}") String allowedMethods,
        @Value("${spring.web.cors.allowed-headers:*}") String allowedHeaders,
        @Value("${spring.web.cors.allow-credentials:true}") boolean allowCredentials
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Split origins and handle wildcard
        List<String> originsList = List.of(allowedOrigins.split(","));
        
        // If allowCredentials is true, we cannot use "*" in allowedOrigins
        // Use allowedOriginPatterns for wildcard, or explicit origins
        if (originsList.size() == 1 && originsList.get(0).equals("*")) {
            if (allowCredentials) {
                // Can't use "*" with credentials, so use patterns and disable credentials
                configuration.setAllowedOriginPatterns(List.of("*"));
                configuration.setAllowCredentials(false);
            } else {
                // Can use "*" without credentials
                configuration.setAllowedOrigins(originsList);
                configuration.setAllowCredentials(false);
            }
        } else {
            // Use explicit origins list
            configuration.setAllowedOrigins(originsList);
            configuration.setAllowCredentials(allowCredentials);
        }
        
        configuration.setAllowedMethods(List.of(allowedMethods.split(",")));
        configuration.setAllowedHeaders(List.of(allowedHeaders.split(",")));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();
        scopesConverter.setAuthorityPrefix("SCOPE_");

        Converter<Jwt, Collection<GrantedAuthority>> aggregateConverter = jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopesConverter.convert(jwt));
            Object permissionsClaim = jwt.getClaims().get("permissions");
            if (permissionsClaim instanceof Collection<?> perms) {
                for (Object p : perms) {
                    if (p != null) {
                        authorities.add(new SimpleGrantedAuthority("SCOPE_" + p.toString()));
                    }
                }
            }
            return authorities;
        };

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(aggregateConverter);
        return converter;
    }

    public static class AudienceValidator implements OAuth2TokenValidator<Jwt> {
        private final String audience;

        AudienceValidator(String audience) {
            this.audience = audience;
        }

        @Override
        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            if (jwt.getAudience() != null && jwt.getAudience().contains(audience)) {
                return OAuth2TokenValidatorResult.success();
            }
            OAuth2Error error = new OAuth2Error("invalid_token", "The required audience is missing", null);
            return OAuth2TokenValidatorResult.failure(error);
        }
    }
}

