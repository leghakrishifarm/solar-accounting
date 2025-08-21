package com.legakrishi.solar.controller;

import com.legakrishi.solar.config.MonitoringProps;
import com.legakrishi.solar.model.EnergySample;
import com.legakrishi.solar.model.MeterKind;
import com.legakrishi.solar.model.Reading;
import com.legakrishi.solar.repository.EnergySampleRepository;
import com.legakrishi.solar.repository.PartnerSiteRepository;
import com.legakrishi.solar.repository.ReadingRepository;
import com.legakrishi.solar.repository.SiteRepository;
import com.legakrishi.solar.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PartnerTelemetryController.class)
@AutoConfigureMockMvc(addFilters = true)
class PartnerTelemetryControllerTest {

    @Autowired MockMvc mvc;

    @MockBean ReadingRepository readingRepo;
    @MockBean PartnerSiteRepository partnerSiteRepo;
    @MockBean UserRepository userRepo;
    @MockBean SiteRepository siteRepo;
    @MockBean EnergySampleRepository energySampleRepo;

    @TestConfiguration
    static class Cfg {
        @Bean MonitoringProps monitoringProps() {
            MonitoringProps p = new MonitoringProps();
            p.setSiteId(1L);
            return p;
        }
    }

    @Test
    @WithMockUser(roles = "PARTNER")
    void monthSummary_fromReading() throws Exception {
        // Arrange READING path: first=500, last=700 -> delta=200
        LocalDateTime t0 = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        Reading r1 = new Reading(); r1.setTs(t0);             r1.setEnergyKwh(500.0); r1.setPowerKw(5.0);
        Reading r2 = new Reading(); r2.setTs(t0.plusDays(1)); r2.setEnergyKwh(700.0); r2.setPowerKw(15.0);

        given(readingRepo.findRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(List.of(r1, r2));

        // Avoid CUF calc (capacity null)
        given(siteRepo.findById(anyLong())).willReturn(Optional.empty());

        mvc.perform(get("/partners/api/month-summary").param("siteId", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.energyMonthKwh").value(200.0))
                .andExpect(jsonPath("$.peakPowerKw").value(15.0));
    }

    @Test
    @WithMockUser(roles = "PARTNER")
    void monthSummary_fromEnergySample_fallback() throws Exception {
        // Force fallback by returning empty READING list
        given(readingRepo.findRange(anyLong(), any(LocalDateTime.class), any(LocalDateTime.class)))
                .willReturn(List.of());

        // ENERGY_SAMPLE fallback for MAIN with dailyAcEnergyKwh 500 -> 700 (delta=200)
        ZoneId IST = ZoneId.of("Asia/Kolkata");
        Instant s1 = LocalDate.now(IST).withDayOfMonth(1).atStartOfDay(IST).toInstant();
        Instant s2 = s1.plusSeconds(3600);

        EnergySample e1 = new EnergySample();
        e1.setSiteId(1L);
        e1.setMeterKind(MeterKind.MAIN);
        e1.setSampleTime(s1);
        e1.setTotalAcPowerKw(12.0);
        e1.setDailyAcEnergyKwh(500.0);

        EnergySample e2 = new EnergySample();
        e2.setSiteId(1L);
        e2.setMeterKind(MeterKind.MAIN);
        e2.setSampleTime(s2);
        e2.setTotalAcPowerKw(25.0);
        e2.setDailyAcEnergyKwh(700.0);

        // IMPORTANT: stub the SAME method name your repo interface actually declares for the controller.
        // If your repo uses "...OrderBySampleTimeAsc", just change the next call to that name.
        given(energySampleRepo.findBySiteIdAndMeterKindAndSampleTimeBetweenOrderBySampleTime(
                anyLong(), any(MeterKind.class), any(Instant.class), any(Instant.class)))
                .willReturn(List.of(e1, e2));

        given(siteRepo.findById(anyLong())).willReturn(Optional.empty());

        mvc.perform(get("/partners/api/month-summary").param("siteId", "1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.energyMonthKwh").value(200.0))
                .andExpect(jsonPath("$.peakPowerKw").value(25.0));
    }
}
