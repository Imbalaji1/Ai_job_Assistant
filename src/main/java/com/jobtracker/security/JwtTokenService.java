package com.jobtracker.security;

import com.jobtracker.entity.UserAccount;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class JwtTokenService {
    private final JwtEncoder encoder;
    private final Duration accessTokenTtl;

    public JwtTokenService(JwtEncoder encoder, Duration accessTokenTtl) {
        this.encoder = encoder;
        this.accessTokenTtl = accessTokenTtl;
    }

    public String issue(UserAccount account) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtConfiguration.ISSUER)
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenTtl))
                .subject(account.getEmail())
                .claim("uid", account.getId())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long expiresInSeconds() {
        return accessTokenTtl.toSeconds();
    }
}
