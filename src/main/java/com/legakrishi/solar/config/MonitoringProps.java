package com.legakrishi.solar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lkf.monitoring")
public class MonitoringProps {

    private Long siteId = 1L;
    private Long deviceId = 1L;

    // offline rule
    private int offlineThresholdMinutes = 10;

    // zero-power rule
    private int zeroWindowMinutes = 5;
    private int zeroMinReadings = 3;
    private double zeroThresholdKw = 0.1;

    private String daylightStart = "09:00";
    private String daylightEnd   = "17:00";

    public Long getSiteId() { return siteId; }
    public void setSiteId(Long siteId) { this.siteId = siteId; }

    public Long getDeviceId() { return deviceId; }
    public void setDeviceId(Long deviceId) { this.deviceId = deviceId; }

    public int getOfflineThresholdMinutes() { return offlineThresholdMinutes; }
    public void setOfflineThresholdMinutes(int offlineThresholdMinutes) { this.offlineThresholdMinutes = offlineThresholdMinutes; }

    public int getZeroWindowMinutes() { return zeroWindowMinutes; }
    public void setZeroWindowMinutes(int zeroWindowMinutes) { this.zeroWindowMinutes = zeroWindowMinutes; }

    public int getZeroMinReadings() { return zeroMinReadings; }
    public void setZeroMinReadings(int zeroMinReadings) { this.zeroMinReadings = zeroMinReadings; }

    public double getZeroThresholdKw() { return zeroThresholdKw; }
    public void setZeroThresholdKw(double zeroThresholdKw) { this.zeroThresholdKw = zeroThresholdKw; }

    public String getDaylightStart() { return daylightStart; }
    public void setDaylightStart(String daylightStart) { this.daylightStart = daylightStart; }

    public String getDaylightEnd() { return daylightEnd; }
    public void setDaylightEnd(String daylightEnd) { this.daylightEnd = daylightEnd; }
}
