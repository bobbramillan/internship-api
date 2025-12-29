package com.bav.internshipapi;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface InternshipRepository extends JpaRepository<Internship, Long> {

    List<Internship> findByDatePostedAfter(LocalDate date);

    int deleteByDatePostedBefore(LocalDate date);

    boolean existsByCompanyAndRoleAndDatePosted(String company, String role, LocalDate datePosted);
}
