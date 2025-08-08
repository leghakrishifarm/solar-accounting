// src/main/java/com/legakrishi/solar/model/MeterReading.java
package com.legakrishi.solar.model;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MeterReading {
    private String meterName;

    // 1. kWh Import
    private double kwhImportStart;
    private double kwhImportEnd;

    // 2. kWh Export
    private double kwhExportStart;
    private double kwhExportEnd;

    // 3. kVAh Import
    private double kvahImportStart;
    private double kvahImportEnd;

    // 4. kVAh Export
    private double kvahExportStart;
    private double kvahExportEnd;

    // 5. kVArh Lead Import Q2
    private double kvarhLeadImportStart;
    private double kvarhLeadImportEnd;

    // 6. kVArh Lead Export Q4
    private double kvarhLeadExportStart;
    private double kvarhLeadExportEnd;

    // 7. kVArh Lag Import Q1
    private double kvarhLagImportStart;
    private double kvarhLagImportEnd;

    // 8. kVArh Lag Export Q3
    private double kvarhLagExportStart;
    private double kvarhLagExportEnd;

    // 9. Import Power Factor
    private double importPowerFactor;

    // 10. Export Power Factor
    private double exportPowerFactor;

    // 11. Import Billing MD kVA
    private double importBillingMD;

    // 12. Export Billing MD kVA
    private double exportBillingMD;

    // Meter Seals
    private String sealOld;
    private String sealNew;

    // Equipment status
    private String meterBodyStatus;
    private String terminalStatus;
    private String testTerminalBlockStatus;
    private String panelBoxStatus;

}
