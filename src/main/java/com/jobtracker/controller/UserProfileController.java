package com.jobtracker.controller;

import com.jobtracker.dto.UserProfileRequest;
import com.jobtracker.dto.UserProfileResponse;
import com.jobtracker.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
public class UserProfileController {
    private final UserProfileService profileService;

    public UserProfileController(UserProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public UserProfileResponse getProfile() {
        return profileService.getProfile();
    }

    @PutMapping
    public UserProfileResponse saveProfile(@Valid @RequestBody UserProfileRequest request) {
        return profileService.saveProfile(request);
    }
}
