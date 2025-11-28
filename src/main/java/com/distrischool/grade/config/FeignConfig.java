package com.distrischool.grade.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Configuração do Feign para propagar tokens JWT entre microserviços
 */
@Configuration
@Slf4j
public class FeignConfig {

    /**
     * Interceptor que adiciona o token JWT do contexto de segurança atual
     * às requisições Feign para outros microserviços
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                
                if (authentication != null && authentication instanceof JwtAuthenticationToken) {
                    JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
                    Jwt jwt = jwtAuth.getToken();
                    
                    if (jwt != null) {
                        String tokenValue = jwt.getTokenValue();
                        template.header("Authorization", "Bearer " + tokenValue);
                        log.debug("Token JWT adicionado à requisição Feign para: {}", template.url());
                    }
                } else {
                    log.warn("Nenhum token JWT encontrado no contexto de segurança para requisição Feign: {}", template.url());
                }
            }
        };
    }
}




