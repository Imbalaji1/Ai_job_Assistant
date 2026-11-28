package com.jobtracker.repository;

import com.jobtracker.entity.ActivityEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long> {
    Page<ActivityEvent> findAllByUserAccountIdOrderByCreatedAtDescIdDesc(Long userAccountId,
                                                                          Pageable pageable);

    long countByUserAccountIdAndEventType(Long userAccountId, String eventType);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update ActivityEvent event "
            + "set event.userAccountId = :userAccountId where event.userAccountId is null")
    int assignUnownedRecords(@org.springframework.data.repository.query.Param("userAccountId")
                             Long userAccountId);
}
