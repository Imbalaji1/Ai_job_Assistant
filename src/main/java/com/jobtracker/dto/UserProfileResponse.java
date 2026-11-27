package com.jobtracker.dto;

import com.jobtracker.entity.UserProfile;

import java.time.LocalDateTime;

public record UserProfileResponse(
        String name,
        String email,
        String phoneNumber,
        LocalDateTime updatedAt) {

    public static UserProfileResponse from(UserProfile profile) {
        return new UserProfileResponse(profile.getName(), profile.getEmail(),
                profile.getPhoneNumber(), profile.getUpdatedAt());
    }
}
