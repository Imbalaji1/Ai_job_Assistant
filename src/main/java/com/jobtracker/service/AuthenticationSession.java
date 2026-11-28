package com.jobtracker.service;

import com.jobtracker.dto.AuthResponse;

public record AuthenticationSession(AuthResponse response, String refreshToken) {
}
