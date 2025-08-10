package com.legakrishi.solar.repository;

import com.legakrishi.solar.model.MeterKind;
import com.legakrishi.solar.model.ReadingDayMeter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReadingDayMeterRepository extends JpaRepository<ReadingDayMeter, Long> {
    Optional<ReadingDayMeter> findBySiteIdAndMeterKindAndDay(Long siteId, MeterKind meter, LocalDate day);

    // >>> This is the one ChartSeriesService is calling <<<
    List<ReadingDayMeter> findBySiteIdAndMeterKindAndDayBetween(
            Long siteId, MeterKind meter, LocalDate start, LocalDate end);
    List<ReadingDayMeter> findBySiteIdAndMeterKindAndDayBetweenOrderByDayAsc(
            Long siteId, MeterKind meterKind, LocalDate from, LocalDate to
    );
    // (Optional helper if you want a site-wide range)
    List<ReadingDayMeter> findBySiteIdAndDayBetween(Long siteId, LocalDate start, LocalDate end);
}
