package com.jobtracker.service;

import com.jobtracker.dto.UserProfileRequest;
import com.jobtracker.dto.UserProfileResponse;
import com.jobtracker.entity.UserProfile;
import com.jobtracker.exception.UserProfileNotFoundException;
import com.jobtracker.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserProfileService {
    private static final long SINGLE_PROFILE_ID = 1L;
    private final UserProfileRepository repository;

    public UserProfileService(UserProfileRepository repository) {
        this.repository = repository;
    }

    public UserProfileResponse getProfile() {
        return repository.findById(SINGLE_PROFILE_ID)
                .map(UserProfileResponse::from)
                .orElseThrow(UserProfileNotFoundException::new);
    }

    @Transactional
    public UserProfileResponse saveProfile(UserProfileRequest request) {
        UserProfile profile = new UserProfile(SINGLE_PROFILE_ID, request.name().trim(),
                request.email().trim(), normalizePhone(request.phoneNumber()));
        return UserProfileResponse.from(repository.save(profile));
    }

    private String normalizePhone(String phoneNumber) {
        return phoneNumber == null || phoneNumber.isBlank() ? null : phoneNumber.trim();
    }
}
