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

    @Column(name = "matching_field", nullable = false, length = 50)
    private String matchingField;

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

    // Manually added getters and setters since Lombok is not working properly
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRegexPattern() {
        return regexPattern;
    }

    public void setRegexPattern(String regexPattern) {
        this.regexPattern = regexPattern;
    }

    public Label getLabel() {
        return label;
    }

    public void setLabel(Label label) {
        this.label = label;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMatchingField() {
        return matchingField;
    }

    public void setMatchingField(String matchingField) {
        this.matchingField = matchingField;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Builder pattern
    public static LabelingRuleBuilder builder() {
        return new LabelingRuleBuilder();
    }

    public static class LabelingRuleBuilder {
        private Long id;
        private String name;
        private String regexPattern;
        private Label label;
        private Boolean isActive;
        private String description;
        private String matchingField;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public LabelingRuleBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public LabelingRuleBuilder name(String name) {
            this.name = name;
            return this;
        }

        public LabelingRuleBuilder regexPattern(String regexPattern) {
            this.regexPattern = regexPattern;
            return this;
        }

        public LabelingRuleBuilder label(Label label) {
            this.label = label;
            return this;
        }

        public LabelingRuleBuilder isActive(Boolean isActive) {
            this.isActive = isActive;
            return this;
        }

        public LabelingRuleBuilder description(String description) {
            this.description = description;
            return this;
        }

        public LabelingRuleBuilder matchingField(String matchingField) {
            this.matchingField = matchingField;
            return this;
        }

        public LabelingRuleBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public LabelingRuleBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public LabelingRule build() {
            LabelingRule rule = new LabelingRule();
            rule.id = this.id;
            rule.name = this.name;
            rule.regexPattern = this.regexPattern;
            rule.label = this.label;
            rule.isActive = this.isActive;
            rule.description = this.description;
            rule.matchingField = this.matchingField;
            rule.createdAt = this.createdAt;
            rule.updatedAt = this.updatedAt;
            return rule;
        }
    }
}
