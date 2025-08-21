package com.legakrishi.solar.repository;

import com.legakrishi.solar.model.EnergySample;
import com.legakrishi.solar.model.MeterKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface EnergySampleRepository extends JpaRepository<EnergySample, Long> {

    // Time range for site & meter
    List<EnergySample> findBySiteIdAndMeterKindAndSampleTimeBetweenOrderBySampleTime(
            Long siteId, MeterKind meterKind, Instant from, Instant to);

    // Latest N by site & meter
    List<EnergySample> findTop1BySiteIdAndMeterKindOrderBySampleTimeDesc(Long siteId, MeterKind meterKind);

    List<EnergySample> findBySiteIdAndMeterKindAndSampleTimeBetweenOrderBySampleTimeAsc(
            Long siteId, MeterKind meterKind, Instant from, Instant to
    );

    // when you just need a time window (no meter kind)
    List<EnergySample> findBySiteIdAndSampleTimeBetweenOrderBySampleTimeAsc(
            Long siteId, Instant from, Instant to
    );
    List<EnergySample> findBySiteIdAndSampleTimeBetweenOrderBySampleTime(
            Long siteId, Instant from, Instant to);
    // For day buckets (we’ll aggregate in service)
    @Query("""
    select s from EnergySample s
     where s.siteId = :siteId
       and s.sampleTime between :from and :to
  """)
    List<EnergySample> findWindow(Long siteId, Instant from, Instant to);

    // ---- Compatibility shim for legacy code that still calls "...Ts..." ----
    // If some classes still reference 'ts', this wrapper will compile and delegate.
    default List<EnergySample> findBySiteIdAndTsBetweenOrderByTsAsc(
            Long siteId, Instant from, Instant to
    ) {
        return findBySiteIdAndSampleTimeBetweenOrderBySampleTimeAsc(siteId, from, to);
    }

    boolean existsBySiteIdAndSampleTimeAndMeterKind(
            Long siteId, Instant sampleTime, MeterKind meterKind);
    Optional<EnergySample> findTopBySiteIdAndMeterKindOrderBySampleTimeDesc(Long siteId, MeterKind meterKind);
    Optional<EnergySample> findTopBySiteIdAndMeterKindAndSampleTimeLessThanEqualOrderBySampleTimeDesc(
            Long siteId, MeterKind meterKind, Instant sampleTime);
}
