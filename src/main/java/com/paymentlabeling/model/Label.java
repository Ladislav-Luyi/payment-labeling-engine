package com.paymentlabeling.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a label that can be assigned to payments
 */
@Entity
@Table(name = "labels")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Label {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", unique = true, nullable = false, length = 100)
    private String name;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
    public static LabelBuilder builder() {
        return new LabelBuilder();
    }

    public static class LabelBuilder {
        private Long id;
        private String name;
        private String description;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public LabelBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public LabelBuilder name(String name) {
            this.name = name;
            return this;
        }

        public LabelBuilder description(String description) {
            this.description = description;
            return this;
        }

        public LabelBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public LabelBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Label build() {
            Label label = new Label();
            label.id = this.id;
            label.name = this.name;
            label.description = this.description;
            label.createdAt = this.createdAt;
            label.updatedAt = this.updatedAt;
            return label;
        }
    }
}
