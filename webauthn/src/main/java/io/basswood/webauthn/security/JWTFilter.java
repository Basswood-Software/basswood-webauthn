package io.basswood.webauthn.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jwt.SignedJWT;
import io.basswood.webauthn.exception.EntityNotFound;
import io.basswood.webauthn.exception.RootException;
import io.basswood.webauthn.exception.TokenValidationError;
import io.basswood.webauthn.model.jwk.JWKEntity;
import io.basswood.webauthn.model.token.Role;
import io.basswood.webauthn.service.JWKService;
import io.basswood.webauthn.service.TokenGenerator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * @author shamualr
 * @since 1.0
 */
@Slf4j
public class JWTFilter extends OncePerRequestFilter {
    public static final String HEADER_AUTHORIZATION = "Authorization";
    private TokenGenerator tokenGenerator;
    private JWKService jwkService;

    private RoleMapping roleMapping;
    private ObjectMapper objectMapper;

    private Boolean disableJwtFilter;

    public JWTFilter(JWKService jwkService, ObjectMapper objectMapper, Boolean disableJwtFilter) {
        this.jwkService = jwkService;
        this.disableJwtFilter = (disableJwtFilter != null) ? disableJwtFilter : false;
        this.tokenGenerator = new TokenGenerator();
        this.roleMapping = new RoleMapping();
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (disableJwtFilter) { // JWT Security is disabled.
            filterChain.doFilter(request, response);
            return;
        }
        URL requestURL = new URL(request.getRequestURL().toString());
        Role role = roleMapping.requireRole(requestURL.getPath());
        if (role == Role.none) { // JWT protection not needed for this API call
            filterChain.doFilter(request, response);
            return;
        }
        try {
            jwtAuthorization(request, role);
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            RootException rootException = switch (ex) {
                case RootException re -> re;
                default -> new RootException(ex);
            };
            response.setHeader("Content-Type", "application/json");
            response.setStatus(rootException.getHttpStatus());
            String path = request.getRequestURI();
            response.getOutputStream().write(objectMapper.writeValueAsString(rootException.toErrorDto(path))
                    .getBytes(StandardCharsets.UTF_8));
        }
    }

    private void jwtAuthorization(HttpServletRequest request, Role role) {
        SignedJWT signedJWT = extractJWT(request);
        String keyID = signedJWT.getHeader().getKeyID();
        Optional<JWKEntity> entityOptional = jwkService.getJWKEntity(keyID);
        if(entityOptional.isEmpty()){
            throw new EntityNotFound(JWKEntity.class, keyID);
        }
        JWK jwk = jwkService.parse(entityOptional.get().getJwkData());
        if (!tokenGenerator.validateSignedJWT(jwk, signedJWT)) {
            throw new TokenValidationError("Invalid token");
        }
        if (!tokenGenerator.hasClaim(signedJWT, TokenGenerator.CLAIM_NAME_ROLES, role.name())) {
            throw new TokenValidationError("Missing necessary role", null, HttpStatus.FORBIDDEN.value());
        }
    }

    private SignedJWT extractJWT(HttpServletRequest request) {
        String header = request.getHeader(HEADER_AUTHORIZATION);
        if (header == null || header.trim().isEmpty()) {
            throw new TokenValidationError("JWT missing");
        }
        header = header.replace("Bearer ", "");
        return tokenGenerator.parseSignedJWT(header);
    }
}