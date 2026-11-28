package com.jobtracker.repository;

import com.jobtracker.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    List<AiConversation> findAllByUserAccountIdOrderByUpdatedAtDesc(Long userAccountId);

    Optional<AiConversation> findByIdAndUserAccountId(Long id, Long userAccountId);

    @Modifying
    @Query("update AiConversation conversation set conversation.userAccountId = :userAccountId "
            + "where conversation.userAccountId is null")
    int assignUnownedRecords(@Param("userAccountId") Long userAccountId);
}
