package com.legakrishi.solar.config;

import com.legakrishi.solar.model.Device;
import com.legakrishi.solar.model.Site;
import com.legakrishi.solar.repository.DeviceRepository;
import com.legakrishi.solar.repository.SiteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Configuration
public class MonitoringBootstrap implements CommandLineRunner {

    private final SiteRepository siteRepo;
    private final DeviceRepository deviceRepo;

    public MonitoringBootstrap(SiteRepository siteRepo, DeviceRepository deviceRepo) {
        this.siteRepo = siteRepo;
        this.deviceRepo = deviceRepo;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (siteRepo.count() == 0) {
            Site site = Site.builder()
                    .name("Legha Krishi Farm – Bhunia")
                    .location("Bhunia, Rajasthan")
                    .build();
            site = siteRepo.save(site);

            String token = UUID.randomUUID().toString().replace("-", "");

            Device dev = Device.builder()
                    .site(site)
                    .name("Gateway #1")
                    .type("GATEWAY")
                    .apiToken(token)
                    .build();
            deviceRepo.save(dev);

            System.out.println("[MonitoringBootstrap] Seeded site + device. API token: " + token);
        }
    }
}
