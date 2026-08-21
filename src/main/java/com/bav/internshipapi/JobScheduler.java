package com.bav.internshipapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        int deactivated = 0;

        LocalDate cutoff = LocalDate.now().minusDays(MAX_AGE_DAYS);

        // Every ID present anywhere in this fetch, regardless of the 29-day cutoff below —
        // used to detect listings the source has dropped entirely.
        Set<String> incomingIds = new HashSet<>();

        for (Job incoming : fetched) {
            incomingIds.add(incoming.getSimplifyId());

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
                if (job.getPostedAt() == null ? incoming.getPostedAt() != null
                        : !job.getPostedAt().equals(incoming.getPostedAt())) {
                    job.setPostedAt(incoming.getPostedAt());
                    changed = true;
                }

                if (changed) {
                    repository.save(job);
                    updated++;
                }
            }
        }

        // Reconcile: a job we still have marked active but that no longer appears
        // anywhere in this fresh snapshot has been fully removed upstream (not just
        // flagged inactive) — Simplify does this when it prunes a listing outright.
        // Without this, such rows never get revisited again and can sit "active"
        // indefinitely, since their (correct, original) datePosted may still be
        // within the 29-day purge window.
        for (Job dbJob : repository.findByIsActiveTrue()) {
            if (!incomingIds.contains(dbJob.getSimplifyId())) {
                dbJob.setActive(false);
                repository.save(dbJob);
                deactivated++;
            }
        }

        logger.info("Sync complete: {} added, {} updated, {} deactivated (removed upstream)", added, updated, deactivated);
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
