package com.jobtracker.repository;

import com.jobtracker.entity.ActivityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {
    Page<ActivityEvent> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    long countByEventType(String eventType);
}
