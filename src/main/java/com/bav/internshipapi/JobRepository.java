package com.bav.internshipapi;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    Optional<Job> findBySimplifyId(String simplifyId);

    List<Job> findByIsActiveTrueAndDatePostedAfter(LocalDate cutoff);

    List<Job> findByIsActiveTrueAndCompanyContainingIgnoreCaseAndDatePostedAfter(String company, LocalDate cutoff);

    List<Job> findByIsActiveTrueAndSponsorshipContainingIgnoreCaseAndDatePostedAfter(String sponsorship, LocalDate cutoff);

    long countByIsActiveTrueAndDatePostedAfter(LocalDate cutoff);

    List<Job> findByDatePostedAfter(LocalDate cutoff);

    // Used to reconcile against a fresh listings.json snapshot — anything active
    // in our DB but missing from the incoming batch has been fully removed upstream.
    List<Job> findByIsActiveTrue();

    void deleteByDatePostedBefore(LocalDate cutoff);
}
