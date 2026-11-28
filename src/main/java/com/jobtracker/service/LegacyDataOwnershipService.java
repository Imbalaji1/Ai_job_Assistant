package com.jobtracker.service;

import com.jobtracker.repository.ActivityEventRepository;
import com.jobtracker.repository.AiConversationRepository;
import com.jobtracker.repository.JobApplicationRepository;
import com.jobtracker.repository.ResumeVersionRepository;
import com.jobtracker.repository.UserProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegacyDataOwnershipService {
    private final JobApplicationRepository applicationRepository;
    private final ResumeVersionRepository resumeRepository;
    private final AiConversationRepository conversationRepository;
    private final ActivityEventRepository activityRepository;
    private final UserProfileRepository profileRepository;

    public LegacyDataOwnershipService(JobApplicationRepository applicationRepository,
                                      ResumeVersionRepository resumeRepository,
                                      AiConversationRepository conversationRepository,
                                      ActivityEventRepository activityRepository,
                                      UserProfileRepository profileRepository) {
        this.applicationRepository = applicationRepository;
        this.resumeRepository = resumeRepository;
        this.conversationRepository = conversationRepository;
        this.activityRepository = activityRepository;
        this.profileRepository = profileRepository;
    }

    @Transactional
    public void assignUnownedRecordsTo(long accountId) {
        applicationRepository.assignUnownedRecords(accountId);
        resumeRepository.assignUnownedRecords(accountId);
        conversationRepository.assignUnownedRecords(accountId);
        activityRepository.assignUnownedRecords(accountId);
        profileRepository.assignUnownedRecords(accountId);
    }
}
