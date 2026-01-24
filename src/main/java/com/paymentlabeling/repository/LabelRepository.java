package com.paymentlabeling.repository;

import com.paymentlabeling.model.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Label entity
 */
@Repository
public interface LabelRepository extends JpaRepository<Label, Long> {

    /**
     * Find a label by its name
     */
    Optional<Label> findByName(String name);

    /**
     * Check if a label exists by name
     */
    boolean existsByName(String name);
}
