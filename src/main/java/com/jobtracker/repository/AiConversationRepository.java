package com.jobtracker.repository;

import com.jobtracker.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    List<AiConversation> findAllByOrderByUpdatedAtDesc();
}
