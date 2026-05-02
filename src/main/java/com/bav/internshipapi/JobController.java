package com.bav.internshipapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    @Autowired
    private JobRepository repository;

    @Autowired
    private SimplifyService simplifyService;

    // GET /api/jobs — all active jobs
    @GetMapping
    public List<Job> getActiveJobs() {
        return repository.findByIsActiveTrue();
    }

    // GET /api/jobs/all — active + inactive
    @GetMapping("/all")
    public List<Job> getAllJobs() {
        return repository.findAll();
    }

    // GET /api/jobs/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Job> getById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/jobs/search?company=Google
    @GetMapping("/search")
    public List<Job> searchByCompany(@RequestParam String company) {
        return repository.findByIsActiveTrueAndCompanyContainingIgnoreCase(company);
    }

    // GET /api/jobs/sponsorship?type=Sponsors
    @GetMapping("/sponsorship")
    public List<Job> filterBySponsorship(@RequestParam String type) {
        return repository.findByIsActiveTrueAndSponsorshipContainingIgnoreCase(type);
    }


    // GET /api/jobs/count
    @GetMapping("/count")
    public long countActive() {
        return repository.countByIsActiveTrue();
    }

    // POST /api/jobs/refresh — manual sync trigger
    @PostMapping("/refresh")
    public ResponseEntity<String> refresh() {
        try {
            List<Job> fetched = simplifyService.fetchJobs();
            if (fetched.isEmpty()) {
                return ResponseEntity.ok("No new data from source (may be cached)");
            }
            int added = 0, updated = 0;
            for (Job incoming : fetched) {
                var existing = repository.findBySimplifyId(incoming.getSimplifyId());
                if (existing.isEmpty()) {
                    repository.save(incoming);
                    added++;
                } else {
                    Job job = existing.get();
                    boolean changed = false;
                    if (job.isActive() != incoming.isActive()) { job.setActive(incoming.isActive()); changed = true; }
                    if (changed) { repository.save(job); updated++; }
                }
            }
            return ResponseEntity.ok("Refreshed: " + added + " added, " + updated + " updated");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
