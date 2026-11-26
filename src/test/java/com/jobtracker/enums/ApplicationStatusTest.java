package com.jobtracker.enums;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApplicationStatusTest {
    @Test
    void parsesStatusWithoutCaseSensitivity() {
        assertEquals(ApplicationStatus.APPLIED, ApplicationStatus.fromValue("applied"));
        assertEquals(ApplicationStatus.INTERVIEW, ApplicationStatus.fromValue("Interview"));
    }

    @Test
    void rejectsUnknownStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> ApplicationStatus.fromValue("Pending"));
    }
}
