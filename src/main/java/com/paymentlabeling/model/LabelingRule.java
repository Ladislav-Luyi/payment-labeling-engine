package com.paymentlabeling.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a labeling rule with regex pattern
 */
@Entity
@Table(name = "labeling_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabelingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "regex_pattern", nullable = false, columnDefinition = "TEXT")
    private String regexPattern;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "label_id", nullable = false)
    private Label label;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
