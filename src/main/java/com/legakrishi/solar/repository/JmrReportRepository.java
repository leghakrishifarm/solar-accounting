// src/main/java/com/legakrishi/solar/repository/JmrReportRepository.java
package com.legakrishi.solar.repository;

import com.legakrishi.solar.model.JmrReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JmrReportRepository extends JpaRepository<JmrReport, Long> {
    List<JmrReport> findByStatus(String status);
    List<JmrReport> findByMonthAndYear(String month, int year);
    Optional<JmrReport> findTopByOrderByReadingDateDesc();
}
