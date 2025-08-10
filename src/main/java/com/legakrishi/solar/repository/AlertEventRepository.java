package com.legakrishi.solar.repository;

import com.legakrishi.solar.model.AlertEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AlertEventRepository extends JpaRepository<AlertEvent, Long> {

    Optional<AlertEvent> findTopByDeviceIdAndTypeOrderByTriggeredAtDesc(Long deviceId, String type);

    long countByDeviceIdAndTypeAndTriggeredAtAfter(Long deviceId, String type, LocalDateTime after);
}
