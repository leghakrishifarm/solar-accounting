package com.legakrishi.solar.kpi;

import com.legakrishi.solar.model.Reading;
import com.legakrishi.solar.model.ReadingDay;
import com.legakrishi.solar.repository.ReadingDayRepository;
import com.legakrishi.solar.repository.ReadingRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.List;

@Service
public class PartnerKpiService {

    private final ReadingRepository readingRepo;
    private final ReadingDayRepository readingDayRepo;

    public PartnerKpiService(ReadingRepository readingRepo, ReadingDayRepository readingDayRepo) {
        this.readingRepo = readingRepo;
        this.readingDayRepo = readingDayRepo;
    }

    public Kpis compute(Long siteId) {
        ZoneId tz = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(tz);
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay().minusSeconds(1);

        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDateTime startOfMonth = firstOfMonth.atStartOfDay();
        LocalDateTime now = LocalDateTime.now(tz);

        // latest reading (for current power & lifetime)
        Reading latest = readingRepo.findFirstBySiteIdOrderByTsDesc(siteId).orElse(null);

        // day window fallback (if no aggregate)
        Reading firstToday = readingRepo
                .findFirstBySiteIdAndTsBetweenOrderByTsAsc(siteId, startOfDay, endOfDay)
                .orElse(null);
        Reading lastToday = readingRepo
                .findFirstBySiteIdAndTsBetweenOrderByTsDesc(siteId, startOfDay, endOfDay)
                .orElse(null);

        // month window (for fallback)
        Reading firstMonth = readingRepo
                .findFirstBySiteIdAndTsBetweenOrderByTsAsc(siteId, startOfMonth, now)
                .orElse(null);
        Reading lastMonth = readingRepo
                .findFirstBySiteIdAndTsBetweenOrderByTsDesc(siteId, startOfMonth, now)
                .orElse(null);

        // compute KPIs
        double currentPower = (latest != null && latest.getPowerKw() != null) ? latest.getPowerKw() : 0.0;

        // prefer daily aggregate if present
        Double energyTodayAgg = readingDayRepo.findBySiteIdAndDay(siteId, today)
                .map(ReadingDay::getEnergyTodayKwh)
                .orElse(null);
        double energyTodayFallback = diffEnergy(firstToday, lastToday);
        double energyToday = (energyTodayAgg != null) ? energyTodayAgg : energyTodayFallback;

        // MONTH: prefer sum of reading_day for the month, fallback to diff
        List<ReadingDay> monthRows = readingDayRepo.findBySiteIdAndDayBetween(siteId, firstOfMonth, today);
        Double energyMonthAgg = monthRows.stream()
                .map(ReadingDay::getEnergyTodayKwh)
                .filter(v -> v != null)
                .reduce(0.0, Double::sum);

        double energyMonthFallback = diffEnergy(firstMonth, lastMonth);
        double energyMonth = (energyMonthAgg != null && energyMonthAgg > 0) ? energyMonthAgg : energyMonthFallback;

        double energyTotal = (latest != null && latest.getEnergyKwh() != null) ? latest.getEnergyKwh() : 0.0;

        String lastUpdated = (latest != null && latest.getTs() != null) ? latest.getTs().toString() : "—";

        return new Kpis(currentPower, energyToday, energyMonth, energyTotal, lastUpdated);
    }

    private double diffEnergy(Reading first, Reading last) {
        if (first == null || last == null) return 0.0;
        Double a = first.getEnergyKwh();
        Double b = last.getEnergyKwh();
        if (a == null || b == null) return 0.0;
        double d = b - a;
        return d < 0 ? 0.0 : d;
    }

    @Data @AllArgsConstructor
    public static class Kpis {
        private double currentPowerKw;
        private double energyTodayKwh;
        private double energyMonthKwh;
        private double energyTotalKwh;
        private String lastUpdated;
    }
}
