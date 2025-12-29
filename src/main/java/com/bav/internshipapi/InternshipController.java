package com.bav.internshipapi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/internships")
public class InternshipController {

    @Autowired
    private InternshipRepository repository;

    @Autowired
    private GitHubService gitHubService;

    @GetMapping
    public List<Internship> getAllInternships() {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        return repository.findByDatePostedAfter(thirtyDaysAgo);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Internship> getInternshipById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/recent")
    public List<Internship> getRecentInternships(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate since) {
        return repository.findByDatePostedAfter(since);
    }

    @GetMapping("/search")
    public List<Internship> searchByCompany(@RequestParam String company) {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        return repository.findByDatePostedAfter(thirtyDaysAgo).stream()
                .filter(i -> i.getCompany().toLowerCase().contains(company.toLowerCase()))
                .toList();
    }

    @PostMapping("/refresh")
    public ResponseEntity<String> refreshInternships() {
        try {
            List<Internship> fetched = gitHubService.fetchInternships();
            LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

            int newCount = 0;
            int skippedOld = 0;

            for (Internship internship : fetched) {
                // Only save if within last 30 days
                if (internship.getDatePosted().isBefore(thirtyDaysAgo)) {
                    skippedOld++;
                    continue;
                }

                boolean exists = repository.existsByCompanyAndRoleAndDatePosted(
                        internship.getCompany(),
                        internship.getRole(),
                        internship.getDatePosted()
                );

                if (!exists) {
                    repository.save(internship);
                    newCount++;
                }
            }

            return ResponseEntity.ok("Refreshed! Added " + newCount + " new internships (skipped " + skippedOld + " older than 30 days)");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/count")
    public long getCount() {
        return repository.count();
    }
}
