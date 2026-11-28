package com.jobtracker.dto;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds,
                           AuthenticatedUserResponse user) {
}
