package com.paymentlabeling.repository;

import com.paymentlabeling.model.Aggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Aggregate entity
 */
@Repository
public interface AggregateRepository extends JpaRepository<Aggregate, Long> {

    /**
     * Find aggregates for a specific label
     */
    List<Aggregate> findByLabelId(Long labelId);

    /**
     * Find aggregates for a specific year
     */
    List<Aggregate> findByYear(Integer year);

    /**
     * Find aggregates for a specific year and month
     */
    List<Aggregate> findByYearAndMonth(Integer year, Integer month);

    /**
     * Find a specific aggregate by label, year, and month
     */
    Optional<Aggregate> findByLabelIdAndYearAndMonth(Long labelId, Integer year, Integer month);
}
