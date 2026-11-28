package com.jobtracker.service;

import com.jobtracker.dto.UserProfileRequest;
import com.jobtracker.dto.UserProfileResponse;
import com.jobtracker.entity.UserProfile;
import com.jobtracker.exception.EmailAlreadyRegisteredException;
import com.jobtracker.exception.UserProfileNotFoundException;
import com.jobtracker.repository.UserAccountRepository;
import com.jobtracker.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserProfileService {
    private final UserProfileRepository repository;
    private final UserAccountRepository accountRepository;
    private final CurrentAccountProvider currentAccountProvider;

    public UserProfileService(UserProfileRepository repository,
                              UserAccountRepository accountRepository,
                              CurrentAccountProvider currentAccountProvider) {
        this.repository = repository;
        this.accountRepository = accountRepository;
        this.currentAccountProvider = currentAccountProvider;
    }

    public UserProfileResponse getProfile() {
        return repository.findByUserAccountId(currentAccountProvider.requireAccountId())
                .map(UserProfileResponse::from)
                .orElseThrow(UserProfileNotFoundException::new);
    }

    @Transactional
    public UserProfileResponse saveProfile(UserProfileRequest request) {
        long accountId = currentAccountProvider.requireAccountId();
        String email = request.email().trim().toLowerCase(java.util.Locale.ROOT);
        var account = accountRepository.findById(accountId)
                .orElseThrow(UserProfileNotFoundException::new);
        if (accountRepository.existsByEmailIgnoreCaseAndIdNot(email, accountId)) {
            throw new EmailAlreadyRegisteredException();
        }
        account.setName(request.name().trim());
        account.setEmail(email);
        accountRepository.save(account);

        UserProfile profile = repository.findByUserAccountId(accountId)
                .orElseGet(() -> new UserProfile(accountId, accountId,
                        request.name().trim(), email, null));
        profile.setUserAccountId(accountId);
        profile.setName(request.name().trim());
        profile.setEmail(email);
        profile.setPhoneNumber(normalizePhone(request.phoneNumber()));
        return UserProfileResponse.from(repository.save(profile));
    }

    private String normalizePhone(String phoneNumber) {
        return phoneNumber == null || phoneNumber.isBlank() ? null : phoneNumber.trim();
    }
}
