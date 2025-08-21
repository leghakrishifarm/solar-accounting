package com.legakrishi.solar.repository;

import com.legakrishi.solar.model.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface SiteRepository extends JpaRepository<Site, Long> {
}
