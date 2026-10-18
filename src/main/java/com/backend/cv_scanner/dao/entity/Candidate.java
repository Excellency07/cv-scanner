package com.backend.cv_scanner.dao.entity;

import com.backend.cv_scanner.model.enums.JobLocationType;
import com.backend.cv_scanner.model.enums.JobType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "full_name")
    private String fullName;

    @Column(nullable = false)
    @Email
    private String email;

    @Column(nullable = false)
    private String phone;

//    @Column(name = "years_of_experience", nullable = false)
//    private int yearsOfExperience;
//
//    @Column(nullable = false)
//    private String location;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "job_type", nullable = false)
//    private JobType jobType;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "job_location_type",  nullable = false)
//    private JobLocationType jobLocationType;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false, unique = true)
    private CVFile file;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Candidate(String fullName, String email, String phone, CVFile file) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.file = file;
    }
}
