package com.bav.internshipapi;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    Optional<Job> findBySimplifyId(String simplifyId);

    List<Job> findByIsActiveTrue();

    List<Job> findByIsActiveTrueAndCompanyContainingIgnoreCase(String company);

    List<Job> findByIsActiveTrueAndSponsorshipContainingIgnoreCase(String sponsorship);

    long countByIsActiveTrue();
}
