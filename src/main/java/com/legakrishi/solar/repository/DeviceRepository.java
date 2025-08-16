package com.legakrishi.solar.repository;

import com.legakrishi.solar.model.Device;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import com.legakrishi.solar.model.MeterKind;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByApiToken(String apiToken);
    Optional<Device> findFirstBySiteIdOrderByIdAsc(Long siteId);

    Optional<Device> findFirstBySiteIdAndDefaultMeterKind(Long siteId, MeterKind defaultMeterKind);
    List<Device> findAllBySiteId(Long siteId);


    @Modifying
    @Query("update Device d set d.lastSeen = CURRENT_TIMESTAMP where d.id = :id")
    int touchLastSeen(@Param("id") Long id);
}
