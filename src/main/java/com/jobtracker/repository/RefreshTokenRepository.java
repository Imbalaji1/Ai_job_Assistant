package com.jobtracker.repository;

import com.jobtracker.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHashAndExpiresAtAfter(String tokenHash,
                                                            LocalDateTime now);

    @Modifying
    @Query("delete from RefreshToken token where token.tokenHash = :tokenHash "
            + "and token.expiresAt > :now")
    int consumeIfActive(@Param("tokenHash") String tokenHash,
                        @Param("now") LocalDateTime now);

    long deleteByTokenHash(String tokenHash);

    boolean existsByTokenHash(String tokenHash);
}
