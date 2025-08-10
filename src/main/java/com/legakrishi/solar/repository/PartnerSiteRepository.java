package com.legakrishi.solar.repository;

import com.legakrishi.solar.model.PartnerSite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartnerSiteRepository extends JpaRepository<PartnerSite, Long> {
    List<PartnerSite> findByPartnerIdAndActiveTrue(Long partnerId);
    boolean existsByPartnerIdAndSiteId(Long partnerId, Long siteId);
}
