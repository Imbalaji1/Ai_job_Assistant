package com.jobtracker.service;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
public class CurrentAccountProvider {
    public long requireAccountId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            Object value = jwtAuthentication.getToken().getClaim("uid");
            if (value instanceof Number accountId && accountId.longValue() > 0) {
                return accountId.longValue();
            }
        }
        throw new IllegalStateException("A signed-in account is required.");
    }
}
