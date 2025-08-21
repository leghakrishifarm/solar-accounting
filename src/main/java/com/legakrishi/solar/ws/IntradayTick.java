package com.legakrishi.solar.ws;

import java.util.Map;

public class IntradayTick {
    public String label;                 // e.g. "14:05"
    public Map<String, Double> series;   // e.g. { MAIN: 12.3, STANDBY: 11.8, CHECK: 12.1 }

    public IntradayTick(String label, Map<String, Double> series) {
        this.label = label;
        this.series = series;
    }

    // no-args constructor (useful for JSON serialization)
    public IntradayTick() {}
}
