package com.paymentlabeling.repository;

import com.paymentlabeling.model.Aggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Aggregate entity
 * Aggregates now represent groups of payments with the same label set for a given month/year
 */
@Repository
public interface AggregateRepository extends JpaRepository<Aggregate, Long> {

    /**
     * Find aggregates for a specific year
     */
    List<Aggregate> findByYear(Integer year);

    /**
     * Find aggregates for a specific year and month
     */
    List<Aggregate> findByYearAndMonth(Integer year, Integer month);

    /**
     * Find aggregates for a specific year and period end day
     */
    List<Aggregate> findByYearAndPeriodEndDay(Integer year, Integer periodEndDay);

    /**
     * Find aggregates for a specific year and month and period end day
     */
    List<Aggregate> findByYearAndMonthAndPeriodEndDay(Integer year, Integer month, Integer periodEndDay);

    /**
     * Find aggregates for a specific period end day
     */
    List<Aggregate> findByPeriodEndDay(Integer periodEndDay);

    /**
     * Find aggregates for a specific year and period end day that is null
     */
    List<Aggregate> findByYearAndPeriodEndDayIsNull(Integer year);

    /**
     * Find aggregates for a specific month and period end day that is null
     */
    List<Aggregate> findByYearAndMonthAndPeriodEndDayIsNull(Integer year, Integer month);

    /**
     * Find aggregates with period end day that is null
     */
    List<Aggregate> findByPeriodEndDayIsNull();

    /**
     * Delete aggregates by a specific period end day
     */
    void deleteByPeriodEndDay(Integer periodEndDay);

    /**
     * Delete aggregates with period end day that is null
     */
    void deleteByPeriodEndDayIsNull();

    /**
     * Find an aggregate by its label set (as a set of label IDs), year, and month
     * This uses a native query to match aggregates with the exact label set
     */
    @Query(value = "SELECT a.* FROM aggregates a " +
            "WHERE a.\"year\" = :year AND a.\"month\" = :month " +
            "AND ((:periodEndDay IS NULL AND a.period_end_day IS NULL) OR a.period_end_day = :periodEndDay) " +
            "AND (SELECT COUNT(*) FROM aggregate_labels WHERE aggregate_id = a.id) = :labelCount " +
            "AND (SELECT COUNT(*) FROM aggregate_labels WHERE aggregate_id = a.id AND label_id IN (:labelIds)) = :labelCount",
            nativeQuery = true)
    Optional<Aggregate> findByLabelSetYearAndMonth(
            @Param("labelIds") List<Long> labelIds,
            @Param("labelCount") int labelCount,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("periodEndDay") Integer periodEndDay);

    /**
     * Find all aggregates that contain any of the specified labels
     */
    @Query("SELECT DISTINCT a FROM Aggregate a JOIN a.labels l WHERE l.id IN :labelIds ORDER BY a.year DESC, a.month DESC")
    List<Aggregate> findByLabelsIn(@Param("labelIds") List<Long> labelIds);

    /**
     * Find aggregates by a single label ID (for backward compatibility if needed)
     */
    @Query("SELECT a FROM Aggregate a JOIN a.labels l WHERE l.id = :labelId ORDER BY a.year DESC, a.month DESC")
    List<Aggregate> findByLabelId(@Param("labelId") Long labelId);

    @Query("SELECT a FROM Aggregate a JOIN a.labels l WHERE l.id = :labelId AND a.periodEndDay IS NULL ORDER BY a.year DESC, a.month DESC")
    List<Aggregate> findByLabelIdAndPeriodEndDayIsNull(@Param("labelId") Long labelId);
}
