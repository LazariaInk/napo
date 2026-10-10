package com.lazar.napo.security;

import com.lazar.napo.exception.InvalidCredentialsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GoogleIdTokenService {

    private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";

    private final NimbusJwtDecoder googleJwtDecoder;
    private final Set<String> allowedClientIds;

    public GoogleIdTokenService(
            @Value("${napo.google.allowed-client-ids:}") String allowedClientIds
    ) {
        this.googleJwtDecoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS_URI).build();
        this.allowedClientIds = Arrays.stream(allowedClientIds.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public Jwt verify(String idToken) {
        if (allowedClientIds.isEmpty()) {
            throw new InvalidCredentialsException("Google login is not configured");
        }

        try {
            Jwt jwt = googleJwtDecoder.decode(idToken);
            validateIssuer(jwt);
            validateAudience(jwt);
            return jwt;
        } catch (JwtException exception) {
            throw new InvalidCredentialsException("Google ID token is invalid");
        }
    }

    private void validateIssuer(Jwt jwt) {
        String issuer = jwt.getIssuer() == null ? null : jwt.getIssuer().toString();

        if (!"https://accounts.google.com".equals(issuer) && !"accounts.google.com".equals(issuer)) {
            throw new InvalidCredentialsException("Google ID token issuer is invalid");
        }
    }

    private void validateAudience(Jwt jwt) {
        boolean accepted = jwt.getAudience().stream().anyMatch(allowedClientIds::contains);

        if (!accepted) {
            throw new InvalidCredentialsException("Google ID token audience is invalid");
        }
    }
}
