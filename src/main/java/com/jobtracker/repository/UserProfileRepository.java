package com.jobtracker.repository;

import com.jobtracker.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
    Optional<UserProfile> findByUserAccountId(Long userAccountId);

    @Modifying
    @Query("update UserProfile profile set profile.userAccountId = :userAccountId "
            + "where profile.userAccountId is null")
    int assignUnownedRecords(@Param("userAccountId") Long userAccountId);
}
