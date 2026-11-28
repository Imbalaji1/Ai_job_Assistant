package com.jobtracker.service;

import com.jobtracker.dto.AuthResponse;
import com.jobtracker.dto.AuthenticatedUserResponse;
import com.jobtracker.dto.LoginRequest;
import com.jobtracker.dto.RegisterRequest;
import com.jobtracker.entity.UserAccount;
import com.jobtracker.entity.UserProfile;
import com.jobtracker.exception.EmailAlreadyRegisteredException;
import com.jobtracker.exception.InvalidCredentialsException;
import com.jobtracker.repository.UserAccountRepository;
import com.jobtracker.repository.UserProfileRepository;
import com.jobtracker.security.JwtTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {
    private final UserAccountRepository accountRepository;
    private final UserProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final CurrentAccountProvider currentAccountProvider;
    private final LegacyDataOwnershipService legacyDataOwnershipService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final String dummyPasswordHash;

    public AuthService(UserAccountRepository accountRepository,
                       UserProfileRepository profileRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService tokenService,
                       CurrentAccountProvider currentAccountProvider,
                       LegacyDataOwnershipService legacyDataOwnershipService,
                       RefreshTokenService refreshTokenService,
                       LoginAttemptLimiter loginAttemptLimiter) {
        this.accountRepository = accountRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.currentAccountProvider = currentAccountProvider;
        this.legacyDataOwnershipService = legacyDataOwnershipService;
        this.refreshTokenService = refreshTokenService;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    @Transactional
    public AuthenticationSession register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (accountRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password must be no longer than 72 UTF-8 bytes.");
        }

        boolean firstAccount = accountRepository.count() == 0;
        UserAccount account = accountRepository.saveAndFlush(new UserAccount(
                request.name().trim(), email, passwordEncoder.encode(request.password())));
        if (firstAccount) {
            legacyDataOwnershipService.assignUnownedRecordsTo(account.getId());
        }

        UserProfile profile = profileRepository.findByUserAccountId(account.getId())
                .orElseGet(() -> new UserProfile(account.getId(), account.getId(),
                        account.getName(), account.getEmail(), null));
        profile.setUserAccountId(account.getId());
        profile.setName(account.getName());
        profile.setEmail(account.getEmail());
        profileRepository.save(profile);
        return createSession(account);
    }

    @Transactional
    public AuthenticationSession login(LoginRequest request, String clientAddress) {
        String email = normalizeEmail(request.email());
        loginAttemptLimiter.checkAllowed(email, clientAddress);
        UserAccount account = accountRepository.findByEmailIgnoreCase(email).orElse(null);
        if (account == null) {
            // Perform a BCrypt comparison for unknown addresses too, reducing timing-based
            // account discovery while returning the same public error as a wrong password.
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            loginAttemptLimiter.recordFailure(email, clientAddress);
            throw new InvalidCredentialsException();
        }
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            loginAttemptLimiter.recordFailure(email, clientAddress);
            throw new InvalidCredentialsException();
        }
        loginAttemptLimiter.recordSuccess(email, clientAddress);
        return createSession(account);
    }

    @Transactional
    public AuthenticationSession refresh(String refreshToken) {
        long accountId = refreshTokenService.consume(refreshToken);
        UserAccount account = accountRepository.findById(accountId)
                .orElseThrow(InvalidCredentialsException::new);
        return createSession(account);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Transactional(readOnly = true)
    public AuthenticatedUserResponse currentUser() {
        long accountId = currentAccountProvider.requireAccountId();
        UserProfile profile = profileRepository.findByUserAccountId(accountId)
                .orElseThrow(InvalidCredentialsException::new);
        return new AuthenticatedUserResponse(accountId, profile.getName(), profile.getEmail());
    }

    private AuthenticationSession createSession(UserAccount account) {
        AuthResponse response = new AuthResponse(tokenService.issue(account), "Bearer",
                tokenService.expiresInSeconds(), new AuthenticatedUserResponse(
                account.getId(), account.getName(), account.getEmail()));
        return new AuthenticationSession(response, refreshTokenService.issue(account.getId()));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
