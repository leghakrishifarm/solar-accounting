package com.legakrishi.solar.repository;

import com.legakrishi.solar.model.AlertDelivery;
import com.legakrishi.solar.model.AlertChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertDeliveryRepository extends JpaRepository<AlertDelivery, Long> {

    List<AlertDelivery> findByAlertIdOrderByAttemptedAtDesc(Long alertId);

    List<AlertDelivery> findTop200ByOrderByAttemptedAtDesc();

    List<AlertDelivery> findTop200ByChannelOrderByAttemptedAtDesc(AlertChannel channel);
}
