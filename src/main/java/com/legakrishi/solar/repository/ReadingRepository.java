package com.legakrishi.solar.repository;

import com.legakrishi.solar.model.MeterKind;
import com.legakrishi.solar.model.Reading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReadingRepository extends JpaRepository<Reading, Long> {

    @Query("""
           select r from Reading r
           where r.site.id = :siteId and r.ts between :start and :end
           order by r.ts asc
           """)
    List<Reading> findRange(Long siteId, LocalDateTime start, LocalDateTime end);

    default List<Reading> findToday(Long siteId) {
        var today = LocalDate.now();
        return findRange(siteId, today.atStartOfDay(), today.plusDays(1).atStartOfDay().minusSeconds(1));
    }

    Optional<Reading> findFirstBySiteIdOrderByTsDesc(Long siteId);

    Optional<Reading> findFirstBySiteIdAndTsBetweenOrderByTsAsc(Long siteId, LocalDateTime start, LocalDateTime end);

    Optional<Reading> findFirstBySiteIdAndTsBetweenOrderByTsDesc(Long siteId, LocalDateTime start, LocalDateTime end);

    // Per meter (explicit)
    List<Reading> findBySiteIdAndMeterKindAndTsBetweenOrderByTsAsc(
            Long siteId, MeterKind meterKind, LocalDateTime start, LocalDateTime end);

    // For legacy rows where meter_kind IS NULL (treated as MAIN)
    List<Reading> findBySiteIdAndTsBetweenAndMeterKindIsNullOrderByTsAsc(
            Long siteId, LocalDateTime start, LocalDateTime end);
}
