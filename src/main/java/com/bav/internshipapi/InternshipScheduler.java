package com.bav.internshipapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

@Service
public class InternshipScheduler {

    private static final Logger logger = LoggerFactory.getLogger(InternshipScheduler.class);

    @Autowired
    private GitHubService gitHubService;

    @Autowired
    private InternshipRepository repository;

    @Scheduled(fixedRate = 3600000, initialDelay = 5000)
    public void pollGitHub() {
        logger.info("Starting GitHub poll...");

        try {
            List<Internship> fetchedInternships = gitHubService.fetchInternships();

            if (fetchedInternships.isEmpty()) {
                logger.info("No new data from GitHub");
                return;
            }

            int newCount = 0;
            int updatedCount = 0;

            for (Internship internship : fetchedInternships) {
                boolean exists = repository.existsByCompanyAndRoleAndDatePosted(
                        internship.getCompany(),
                        internship.getRole(),
                        internship.getDatePosted()
                );

                if (!exists) {
                    repository.save(internship);
                    newCount++;
                } else {
                    updatedCount++;
                }
            }

            logger.info("Poll complete: {} new, {} existing", newCount, updatedCount);

        } catch (Exception e) {
            logger.error("Error during GitHub poll: " + e.getMessage(), e);
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldInternships() {
        logger.info("Starting cleanup of old internships...");

        try {
            LocalDate cutoffDate = LocalDate.now().minusDays(90);
            int deleted = repository.deleteByDatePostedBefore(cutoffDate);
            logger.info("Deleted {} old internships posted before {}", deleted, cutoffDate);
        } catch (Exception e) {
            logger.error("Error during cleanup: " + e.getMessage(), e);
        }
    }
}
