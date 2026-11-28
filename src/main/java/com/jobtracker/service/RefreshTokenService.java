package com.jobtracker.service;

import com.jobtracker.entity.RefreshToken;
import com.jobtracker.exception.InvalidCredentialsException;
import com.jobtracker.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class RefreshTokenService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final RefreshTokenRepository repository;
    private final Duration lifetime;

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${app.security.refresh-token-ttl:P30D}") Duration lifetime) {
        if (lifetime.isNegative() || lifetime.isZero()) {
            throw new IllegalArgumentException("Refresh-token lifetime must be positive.");
        }
        this.repository = repository;
        this.lifetime = lifetime;
    }

    @Transactional
    public String issue(long userAccountId) {
        byte[] randomBytes = new byte[64];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        repository.save(new RefreshToken(userAccountId, hash(rawToken),
                LocalDateTime.now().plus(lifetime)));
        return rawToken;
    }

    @Transactional
    public long consume(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidCredentialsException();
        }
        String tokenHash = hash(rawToken);
        LocalDateTime now = LocalDateTime.now();
        RefreshToken stored = repository.findByTokenHashAndExpiresAtAfter(tokenHash, now)
                .orElseThrow(InvalidCredentialsException::new);
        if (repository.consumeIfActive(tokenHash, now) != 1) {
            throw new InvalidCredentialsException();
        }
        return stored.getUserAccountId();
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken != null && !rawToken.isBlank()) {
            repository.deleteByTokenHash(hash(rawToken));
        }
    }

    public long lifetimeSeconds() {
        return lifetime.toSeconds();
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available.", exception);
        }
    }
}
