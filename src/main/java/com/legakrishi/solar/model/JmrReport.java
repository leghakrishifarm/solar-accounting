package com.legakrishi.solar.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "jmr_reports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JmrReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String plantName;
    private String month;
    private int year;
    private LocalDate readingDate;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "meterName", column = @Column(name = "main_meter_name")),
            @AttributeOverride(name = "kwhImportStart", column = @Column(name = "main_kwh_import_start")),
            @AttributeOverride(name = "kwhImportEnd", column = @Column(name = "main_kwh_import_end")),
            @AttributeOverride(name = "kwhExportStart", column = @Column(name = "main_kwh_export_start")),
            @AttributeOverride(name = "kwhExportEnd", column = @Column(name = "main_kwh_export_end")),
            @AttributeOverride(name = "kvahImportStart", column = @Column(name = "main_kvah_import_start")),
            @AttributeOverride(name = "kvahImportEnd", column = @Column(name = "main_kvah_import_end")),
            @AttributeOverride(name = "kvahExportStart", column = @Column(name = "main_kvah_export_start")),
            @AttributeOverride(name = "kvahExportEnd", column = @Column(name = "main_kvah_export_end")),
            @AttributeOverride(name = "kvarhLeadImportStart", column = @Column(name = "main_kvarh_lead_import_start")),
            @AttributeOverride(name = "kvarhLeadImportEnd", column = @Column(name = "main_kvarh_lead_import_end")),
            @AttributeOverride(name = "kvarhLeadExportStart", column = @Column(name = "main_kvarh_lead_export_start")),
            @AttributeOverride(name = "kvarhLeadExportEnd", column = @Column(name = "main_kvarh_lead_export_end")),
            @AttributeOverride(name = "kvarhLagImportStart", column = @Column(name = "main_kvarh_lag_import_start")),
            @AttributeOverride(name = "kvarhLagImportEnd", column = @Column(name = "main_kvarh_lag_import_end")),
            @AttributeOverride(name = "kvarhLagExportStart", column = @Column(name = "main_kvarh_lag_export_start")),
            @AttributeOverride(name = "kvarhLagExportEnd", column = @Column(name = "main_kvarh_lag_export_end")),
            @AttributeOverride(name = "importPowerFactor", column = @Column(name = "main_import_power_factor")),
            @AttributeOverride(name = "exportPowerFactor", column = @Column(name = "main_export_power_factor")),
            @AttributeOverride(name = "importBillingMD", column = @Column(name = "main_import_billingmd")),
            @AttributeOverride(name = "exportBillingMD", column = @Column(name = "main_export_billingmd")),
            @AttributeOverride(name = "sealOld", column = @Column(name = "main_seal_old")),
            @AttributeOverride(name = "sealNew", column = @Column(name = "main_seal_new")),
            @AttributeOverride(name = "meterBodyStatus", column = @Column(name = "main_meter_body_status")),
            @AttributeOverride(name = "terminalStatus", column = @Column(name = "main_terminal_status")),
            @AttributeOverride(name = "testTerminalBlockStatus", column = @Column(name = "main_test_terminal_block_status")),
            @AttributeOverride(name = "panelBoxStatus", column = @Column(name = "main_panel_box_status"))
    })
    private MeterReading mainMeter;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "meterName", column = @Column(name = "check_meter_name")),
            @AttributeOverride(name = "kwhImportStart", column = @Column(name = "check_kwh_import_start")),
            @AttributeOverride(name = "kwhImportEnd", column = @Column(name = "check_kwh_import_end")),
            @AttributeOverride(name = "kwhExportStart", column = @Column(name = "check_kwh_export_start")),
            @AttributeOverride(name = "kwhExportEnd", column = @Column(name = "check_kwh_export_end")),
            @AttributeOverride(name = "kvahImportStart", column = @Column(name = "check_kvah_import_start")),
            @AttributeOverride(name = "kvahImportEnd", column = @Column(name = "check_kvah_import_end")),
            @AttributeOverride(name = "kvahExportStart", column = @Column(name = "check_kvah_export_start")),
            @AttributeOverride(name = "kvahExportEnd", column = @Column(name = "check_kvah_export_end")),
            @AttributeOverride(name = "kvarhLeadImportStart", column = @Column(name = "check_kvarh_lead_import_start")),
            @AttributeOverride(name = "kvarhLeadImportEnd", column = @Column(name = "check_kvarh_lead_import_end")),
            @AttributeOverride(name = "kvarhLeadExportStart", column = @Column(name = "check_kvarh_lead_export_start")),
            @AttributeOverride(name = "kvarhLeadExportEnd", column = @Column(name = "check_kvarh_lead_export_end")),
            @AttributeOverride(name = "kvarhLagImportStart", column = @Column(name = "check_kvarh_lag_import_start")),
            @AttributeOverride(name = "kvarhLagImportEnd", column = @Column(name = "check_kvarh_lag_import_end")),
            @AttributeOverride(name = "kvarhLagExportStart", column = @Column(name = "check_kvarh_lag_export_start")),
            @AttributeOverride(name = "kvarhLagExportEnd", column = @Column(name = "check_kvarh_lag_export_end")),
            @AttributeOverride(name = "importPowerFactor", column = @Column(name = "check_import_power_factor")),
            @AttributeOverride(name = "exportPowerFactor", column = @Column(name = "check_export_power_factor")),
            @AttributeOverride(name = "importBillingMD", column = @Column(name = "check_import_billingmd")),
            @AttributeOverride(name = "exportBillingMD", column = @Column(name = "check_export_billingmd")),
            @AttributeOverride(name = "sealOld", column = @Column(name = "check_seal_old")),
            @AttributeOverride(name = "sealNew", column = @Column(name = "check_seal_new")),
            @AttributeOverride(name = "meterBodyStatus", column = @Column(name = "check_meter_body_status")),
            @AttributeOverride(name = "terminalStatus", column = @Column(name = "check_terminal_status")),
            @AttributeOverride(name = "testTerminalBlockStatus", column = @Column(name = "check_test_terminal_block_status")),
            @AttributeOverride(name = "panelBoxStatus", column = @Column(name = "check_panel_box_status"))
    })
    private MeterReading checkMeter;

    private String remarks;
    private String status; // DRAFT, APPROVED
    private String pdfPath; // Path to generated PDF

    // NEW: Path to uploaded and *approved* (signed) PDF
    private String approvedPdfPath;
}
