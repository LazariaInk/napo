package com.lazar.napo.service;

import com.lazar.napo.dto.AuthResponse;
import com.lazar.napo.dto.GoogleLoginRequest;
import com.lazar.napo.dto.LoginRequest;
import com.lazar.napo.dto.RegisterRequest;
import com.lazar.napo.entity.UserAccount;
import com.lazar.napo.entity.UserAuthProvider;
import com.lazar.napo.entity.UserPlan;
import com.lazar.napo.entity.UserRole;
import com.lazar.napo.exception.DuplicateResourceException;
import com.lazar.napo.exception.InvalidCredentialsException;
import com.lazar.napo.repository.UserAccountRepository;
import com.lazar.napo.security.GoogleIdTokenService;
import com.lazar.napo.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleIdTokenService googleIdTokenService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.email());

        if (userAccountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateResourceException("An account with this email already exists");
        }

        UserAccount user = new UserAccount();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setRole(UserRole.USER);
        user.setPlan(UserPlan.FREE);
        user.setAuthProvider(UserAuthProvider.LOCAL);
        user.setEmailVerified(false);

        return toResponse(userAccountRepository.save(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return toResponse(user);
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        Jwt googleToken = googleIdTokenService.verify(request.idToken());
        String email = normalizeEmail(requiredClaim(googleToken, "email"));
        String providerUserId = googleToken.getSubject();
        String displayName = resolveDisplayName(googleToken, email);
        boolean emailVerified = Boolean.TRUE.equals(googleToken.getClaim("email_verified"));

        if (!emailVerified) {
            throw new InvalidCredentialsException("Google account email is not verified");
        }

        UserAccount user = userAccountRepository.findByEmailIgnoreCase(email)
                .map(existingUser -> updateGoogleUser(existingUser, providerUserId, displayName, emailVerified))
                .orElseGet(() -> createGoogleUser(email, providerUserId, displayName, emailVerified));

        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse findByEmail(String email) {
        UserAccount user = userAccountRepository.findByEmailIgnoreCase(normalizeEmail(email))
                .orElseThrow(() -> new InvalidCredentialsException("Authenticated user was not found"));
        return toResponse(user);
    }

    private UserAccount createGoogleUser(
            String email,
            String providerUserId,
            String displayName,
            boolean emailVerified
    ) {
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setDisplayName(displayName);
        user.setRole(UserRole.USER);
        user.setPlan(UserPlan.FREE);
        user.setAuthProvider(UserAuthProvider.GOOGLE);
        user.setProviderUserId(providerUserId);
        user.setEmailVerified(emailVerified);
        return userAccountRepository.save(user);
    }

    private UserAccount updateGoogleUser(
            UserAccount user,
            String providerUserId,
            String displayName,
            boolean emailVerified
    ) {
        user.setAuthProvider(UserAuthProvider.GOOGLE);
        user.setProviderUserId(providerUserId);
        user.setEmailVerified(emailVerified);

        if (user.getDisplayName() == null || user.getDisplayName().isBlank()) {
            user.setDisplayName(displayName);
        }

        return userAccountRepository.save(user);
    }

    private AuthResponse toResponse(UserAccount user) {
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole(),
                user.getPlan() == null ? UserPlan.FREE : user.getPlan(),
                user.getAuthProvider() == null ? UserAuthProvider.LOCAL : user.getAuthProvider(),
                Boolean.TRUE.equals(user.getEmailVerified()),
                jwtService.createAccessToken(user)
        );
    }

    private String requiredClaim(Jwt jwt, String claimName) {
        Object value = jwt.getClaim(claimName);

        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }

        throw new InvalidCredentialsException("Google ID token is missing required claim: " + claimName);
    }

    private String resolveDisplayName(Jwt jwt, String email) {
        Object name = jwt.getClaim("name");

        if (name instanceof String nameValue && !nameValue.isBlank()) {
            return nameValue.trim();
        }

        return email;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}