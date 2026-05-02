package com.bav.internshipapi;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "jobs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Simplify's unique ID — used for dedup and status updates
    @Column(name = "simplify_id", unique = true, nullable = false)
    private String simplifyId;

    @Column(nullable = false)
    private String company;

    @Column(nullable = false)
    private String title;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_locations", joinColumns = @JoinColumn(name = "job_id"))
    @Column(name = "location")
    private List<String> locations;

    @Column(name = "application_url", length = 1000)
    private String applicationUrl;

    @Column(name = "date_posted")
    private LocalDate datePosted;

    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "sponsorship")
    private String sponsorship;

    @Column(name = "created_at")
    private LocalDate createdAt;

    @Column(name = "updated_at")
    private LocalDate updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDate.now();
        updatedAt = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDate.now();
    }
}
