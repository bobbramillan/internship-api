package com.bav.internshipapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class JobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(JobScheduler.class);

    @Autowired
    private SimplifyService simplifyService;

    @Autowired
    private JobRepository repository;

    private static final int MAX_AGE_DAYS = 29;

    // listings.json is updated every 30 minutes by Simplify's bot, so poll every 30 min
    @Scheduled(fixedRate = 1800000, initialDelay = 5000)
    @Transactional
    public void syncJobs() {
        logger.info("Starting sync from listings.json...");

        List<Job> fetched = simplifyService.fetchJobs();
        if (fetched.isEmpty()) {
            logger.info("No data returned (either 304 Not Modified or fetch error)");
            return;
        }

        int added = 0;
        int updated = 0;

        LocalDate cutoff = LocalDate.now().minusDays(MAX_AGE_DAYS);

        for (Job incoming : fetched) {
            if (incoming.getDatePosted() == null || !incoming.getDatePosted().isAfter(cutoff)) continue;
            Optional<Job> existing = repository.findBySimplifyId(incoming.getSimplifyId());

            if (existing.isEmpty()) {
                repository.save(incoming);
                added++;
            } else {
                // Update active status and url in case they changed
                Job job = existing.get();
                boolean changed = false;

                if (job.isActive() != incoming.isActive()) {
                    job.setActive(incoming.isActive());
                    changed = true;
                }
                if (!strEqual(job.getApplicationUrl(), incoming.getApplicationUrl())) {
                    job.setApplicationUrl(incoming.getApplicationUrl());
                    changed = true;
                }
                if (!strEqual(job.getSponsorship(), incoming.getSponsorship())) {
                    job.setSponsorship(incoming.getSponsorship());
                    changed = true;
                }

                if (changed) {
                    repository.save(job);
                    updated++;
                }
            }
        }

        logger.info("Sync complete: {} added, {} updated", added, updated);
    }

    // runs at 3am daily to hard-delete jobs older than 29 days
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeOldJobs() {
        LocalDate cutoff = LocalDate.now().minusDays(MAX_AGE_DAYS);
        repository.deleteByDatePostedBefore(cutoff);
        logger.info("Purged jobs older than {} days", MAX_AGE_DAYS);
    }

    private boolean strEqual(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }
}
