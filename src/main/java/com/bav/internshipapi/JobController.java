package com.bav.internshipapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    @Autowired
    private JobRepository repository;

    @Autowired
    private JobScheduler jobScheduler;

    private static final int MAX_AGE_DAYS = 29;

    private LocalDate cutoff() {
        return LocalDate.now().minusDays(MAX_AGE_DAYS);
    }

    // GET /api/jobs — active jobs posted within the last 29 days
    @GetMapping
    public List<Job> getActiveJobs() {
        return repository.findByIsActiveTrueAndDatePostedAfter(cutoff());
    }

    // GET /api/jobs/all — active + inactive, still limited to 29 days
    @GetMapping("/all")
    public List<Job> getAllJobs() {
        return repository.findByDatePostedAfter(cutoff());
    }

    // GET /api/jobs/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Job> getById(@PathVariable Long id) {
        return repository.findById(id)
                .filter(job -> job.getDatePosted() != null && job.getDatePosted().isAfter(cutoff()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/jobs/search?company=Google
    @GetMapping("/search")
    public List<Job> searchByCompany(@RequestParam String company) {
        return repository.findByIsActiveTrueAndCompanyContainingIgnoreCaseAndDatePostedAfter(company, cutoff());
    }

    // GET /api/jobs/sponsorship?type=Sponsors
    @GetMapping("/sponsorship")
    public List<Job> filterBySponsorship(@RequestParam String type) {
        return repository.findByIsActiveTrueAndSponsorshipContainingIgnoreCaseAndDatePostedAfter(type, cutoff());
    }

    // GET /api/jobs/count
    @GetMapping("/count")
    public long countActive() {
        return repository.countByIsActiveTrueAndDatePostedAfter(cutoff());
    }

    // POST /api/jobs/refresh — manual sync trigger.
    // Delegates to JobScheduler.syncJobs() rather than keeping its own copy of the
    // sync logic — a second copy here previously drifted out of sync (literally):
    // it only updated `active` and skipped url/sponsorship/postedAt updates and the
    // orphan-reconciliation pass, so manual syncs didn't get the same fixes as the
    // scheduled ones.
    @PostMapping("/refresh")
    public ResponseEntity<String> refresh() {
        try {
            jobScheduler.syncJobs();
            return ResponseEntity.ok("Sync complete");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
