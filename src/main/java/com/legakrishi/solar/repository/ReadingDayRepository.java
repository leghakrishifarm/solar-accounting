package com.legakrishi.solar.repository;

import com.legakrishi.solar.model.ReadingDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.time.LocalDate;

public interface ReadingDayRepository extends JpaRepository<ReadingDay, Long> {
    Optional<ReadingDay> findBySiteIdAndDay(Long siteId, LocalDate day);
    List<ReadingDay> findBySiteIdAndDayBetween(Long siteId, LocalDate start, LocalDate end);
}
