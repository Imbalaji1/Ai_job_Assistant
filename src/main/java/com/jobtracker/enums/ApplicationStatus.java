package com.jobtracker.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum ApplicationStatus {
    APPLIED,
    INTERVIEW,
    SELECTED,
    REJECTED,
    WITHDRAWN;

    @JsonCreator
    public static ApplicationStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Status is required.");
        }

        try {
            return ApplicationStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Status must be Applied, Interview, Selected, Rejected, or Withdrawn.");
        }
    }

    @JsonValue
    public String toJsonValue() {
        String lowerCase = name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lowerCase.charAt(0)) + lowerCase.substring(1);
    }
}
