package com.legakrishi.solar.service;

import com.legakrishi.solar.model.*;
import com.legakrishi.solar.repository.ReadingDayRepository;
import com.legakrishi.solar.repository.ReadingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.legakrishi.solar.repository.ReadingDayMeterRepository;
import java.math.BigDecimal;
import java.util.ArrayList;

@Service
public class AggregationService {

    private final ReadingRepository readingRepo;
    private final ReadingDayRepository dayRepo;
    private final ReadingDayMeterRepository rdmRepo;

    public AggregationService(ReadingRepository readingRepo, ReadingDayRepository dayRepo, ReadingDayMeterRepository rdmRepo) {
        this.readingRepo = readingRepo;
        this.dayRepo = dayRepo;
        this.rdmRepo = rdmRepo;
    }

    @Transactional
    public ReadingDay aggregateTodayForSite(Long siteId) {
        var tz = ZoneId.of("Asia/Kolkata");
        var today = LocalDate.now(tz);

        // get all today's readings (ordered asc by repo), then compute
        List<Reading> list = readingRepo.findToday(siteId);
        double energyToday = 0.0;
        double maxPower = 0.0;

        if (!list.isEmpty()) {
            var first = list.get(0).getEnergyKwh();
            var last  = list.get(list.size() - 1).getEnergyKwh();
            if (first != null && last != null && last >= first) {
                energyToday = last - first;
            }
            maxPower = list.stream()
                    .map(Reading::getPowerKw)
                    .filter(v -> v != null)
                    .max(Comparator.naturalOrder())
                    .orElse(0.0);
        }

        Optional<ReadingDay> existing = dayRepo.findBySiteIdAndDay(siteId, today);
        ReadingDay rd = existing.orElseGet(() ->
                ReadingDay.builder()
                        .site(list.isEmpty() ? null : list.get(0).getSite())
                        .day(today)
                        .build()
        );

        // if site was null (no readings today), try to set site from latest reading overall (optional)
        if (rd.getSite() == null && !list.isEmpty()) {
            rd.setSite(list.get(0).getSite());
        }

        rd.setEnergyTodayKwh(energyToday);
        rd.setMaxPowerKw(maxPower);
        rd.setLastTs(list.isEmpty() ? null : list.get(list.size() - 1).getTs());

        return dayRepo.save(rd);
    }

    @Transactional
    public ReadingDayMeter aggregateDayPerMeter(Long siteId, LocalDate day, MeterKind meter) {
        ZoneId IST = ZoneId.of("Asia/Kolkata");
        var start = day.atStartOfDay(IST).toLocalDateTime();
        var end   = day.plusDays(1).atStartOfDay(IST).toLocalDateTime();

        // Fetch meter-specific rows
        List<Reading> list = readingRepo
                .findBySiteIdAndMeterKindAndTsBetweenOrderByTsAsc(siteId, meter, start, end);

        // For legacy MAIN where meter_kind is NULL, merge them in
        if (meter == MeterKind.MAIN) {
            List<Reading> legacy = readingRepo
                    .findBySiteIdAndTsBetweenAndMeterKindIsNullOrderByTsAsc(siteId, start, end);
            if (!legacy.isEmpty()) {
                var merged = new ArrayList<Reading>(list.size() + legacy.size());
                merged.addAll(list);
                merged.addAll(legacy);
                merged.sort(Comparator.comparing(Reading::getTs));
                list = merged;
            }
        }

        // Nothing to aggregate
        if (list.isEmpty()) {
            // Upsert an empty row with zeros to keep continuity
            ReadingDayMeter rdm = rdmRepo.findBySiteIdAndMeterKindAndDay(siteId, meter, day)
                    .orElse(ReadingDayMeter.builder()
                            .site(new Site()) // will be overwritten below
                            .meterKind(meter)
                            .day(day)
                            .build());

            // attach site without loading heavy graph
            Site s = new Site(); s.setId(siteId);
            rdm.setSite(s);
            rdm.setAcActiveEnergyKwh(BigDecimal.ZERO);
            rdm.setAcExportEnergyKwh(BigDecimal.ZERO);
            rdm.setAcImportEnergyKwh(BigDecimal.ZERO);
            rdm.setDcEnergyKwh(BigDecimal.ZERO);
            rdm.setMaxAcPowerKw(BigDecimal.ZERO);
            rdm.setLastTs(null);
            return rdmRepo.save(rdm);
        }

        // Helper lambdas to pick BigDecimal max/min ignoring nulls
        var maxBD = (java.util.function.Function<java.util.stream.Stream<BigDecimal>, BigDecimal>)
                stream -> stream.filter(java.util.Objects::nonNull).max(BigDecimal::compareTo).orElse(null);
        var minBD = (java.util.function.Function<java.util.stream.Stream<BigDecimal>, BigDecimal>)
                stream -> stream.filter(java.util.Objects::nonNull).min(BigDecimal::compareTo).orElse(null);

        // Prefer new fields; fallback to cumulative deltas; fallback to legacy powerKw
        BigDecimal dailyAc = null, dailyExp = null, dailyImp = null, dailyDc = null, maxPow = null;

        // 1) AC Active Energy (daily)
        // prefer max(dailyAcActiveEnergyKwh)
        dailyAc = maxBD.apply(list.stream().map(Reading::getDailyAcActiveEnergyKwh));
        if (dailyAc == null) {
            // fallback: delta of cumulative totalAcActiveEnergyKwh
            BigDecimal maxTot = maxBD.apply(list.stream().map(Reading::getTotalAcActiveEnergyKwh));
            BigDecimal minTot = minBD.apply(list.stream().map(Reading::getTotalAcActiveEnergyKwh));
            if (maxTot != null && minTot != null) dailyAc = maxTot.subtract(minTot);
        }
        if (dailyAc == null) {
            // final fallback: approximate from legacy energyTodayKwh if you used it elsewhere (optional)
            // leave null -> will set ZERO below
        }

        // 2) AC Export Energy (daily)
        dailyExp = maxBD.apply(list.stream().map(Reading::getDailyAcActiveExportEnergyKwh));
        if (dailyExp == null) {
            BigDecimal maxTot = maxBD.apply(list.stream().map(Reading::getTotalAcActiveExportEnergyKwh));
            BigDecimal minTot = minBD.apply(list.stream().map(Reading::getTotalAcActiveExportEnergyKwh));
            if (maxTot != null && minTot != null) dailyExp = maxTot.subtract(minTot);
        }

        // 3) AC Import Energy (daily)
        dailyImp = maxBD.apply(list.stream().map(Reading::getDailyAcActiveImportEnergyKwh));
        if (dailyImp == null) {
            BigDecimal maxTot = maxBD.apply(list.stream().map(Reading::getTotalAcActiveImportEnergyKwh));
            BigDecimal minTot = minBD.apply(list.stream().map(Reading::getTotalAcActiveImportEnergyKwh));
            if (maxTot != null && minTot != null) dailyImp = maxTot.subtract(minTot);
        }

        // 4) DC Energy (daily)
        dailyDc = maxBD.apply(list.stream().map(Reading::getDailyDcEnergyKwh));
        if (dailyDc == null) {
            BigDecimal maxTot = maxBD.apply(list.stream().map(Reading::getTotalDcEnergyKwh));
            BigDecimal minTot = minBD.apply(list.stream().map(Reading::getTotalDcEnergyKwh));
            if (maxTot != null && minTot != null) dailyDc = maxTot.subtract(minTot);
        }

        // Max AC Power (kW): prefer totalAcActivePowerKw; fallback to legacy powerKw
        BigDecimal maxNew = maxBD.apply(list.stream().map(Reading::getTotalAcActivePowerKw));
        if (maxNew != null) {
            maxPow = maxNew;
        } else {
            // legacy double -> convert
            Double maxLegacy = list.stream().map(Reading::getPowerKw).filter(java.util.Objects::nonNull)
                    .max(Double::compareTo).orElse(null);
            if (maxLegacy != null) maxPow = BigDecimal.valueOf(maxLegacy);
        }

        // Default zeros if still null
        if (dailyAc == null) dailyAc = BigDecimal.ZERO;
        if (dailyExp == null) dailyExp = BigDecimal.ZERO;
        if (dailyImp == null) dailyImp = BigDecimal.ZERO;
        if (dailyDc == null) dailyDc = BigDecimal.ZERO;
        if (maxPow == null) maxPow = BigDecimal.ZERO;

        // Upsert
        ReadingDayMeter rdm = rdmRepo.findBySiteIdAndMeterKindAndDay(siteId, meter, day)
                .orElse(ReadingDayMeter.builder()
                        .site(new Site())
                        .meterKind(meter)
                        .day(day)
                        .build());

        Site s = new Site(); s.setId(siteId);
        rdm.setSite(s);
        rdm.setAcActiveEnergyKwh(dailyAc);
        rdm.setAcExportEnergyKwh(dailyExp);
        rdm.setAcImportEnergyKwh(dailyImp);
        rdm.setDcEnergyKwh(dailyDc);
        rdm.setMaxAcPowerKw(maxPow);
        rdm.setLastTs(list.get(list.size()-1).getTs());

        return rdmRepo.save(rdm);
    }

}
