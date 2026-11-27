package com.jobtracker.repository;

import com.jobtracker.entity.AiMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {
    List<AiMessage> findAllByConversationIdOrderByCreatedAtAscIdAsc(Long conversationId);

    List<AiMessage> findTop20ByConversationIdOrderByCreatedAtDescIdDesc(Long conversationId);
}
