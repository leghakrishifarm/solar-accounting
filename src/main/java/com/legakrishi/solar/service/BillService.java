package com.legakrishi.solar.service;

import com.legakrishi.solar.model.Bill;
import com.legakrishi.solar.model.JmrReport;
import com.legakrishi.solar.model.MeterReading;
import com.legakrishi.solar.repository.BillRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class BillService {

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private IncomeService incomeService; // assuming you have IncomeService to track income

    private static final double EXPORT_TARIFF = 3.14; // DISCOM -> SPD (SPD imports)
    private static final double IMPORT_TARIFF = 3.14; // SPD -> DISCOM (SPD exports)
    private static final double KVARH_TARIFF = 0.17;

    public Bill generateBillFromJmr(JmrReport jmr) {
        MeterReading mainMeter = jmr.getMainMeter();
        if (mainMeter == null) {
            throw new IllegalStateException("Main meter missing for JMR id: " + jmr.getId());
        }

        Bill bill = new Bill();
        bill.setJmrReport(jmr);
        bill.setMonth(jmr.getMonth());
        bill.setYear(jmr.getYear());
        bill.setBillDate(LocalDate.now());

        // --- Energy (kWh) diffs (no swapping) ---
        double totalKwhImport = safeDiff(mainMeter.getKwhImportEnd(), mainMeter.getKwhImportStart()); // meter IMPORT = SPD export qty
        double totalKwhExport = safeDiff(mainMeter.getKwhExportEnd(), mainMeter.getKwhExportStart()); // meter EXPORT = SPD import qty

        bill.setTotalKwhImport(round(totalKwhImport)); // SPD → DISCOM
        bill.setTotalKwhExport(round(totalKwhExport)); // DISCOM → SPD

        bill.setImportTariff(IMPORT_TARIFF);
        bill.setExportTariff(EXPORT_TARIFF);

        // Amounts (positive line items)
        double amountOnImport = totalKwhImport * IMPORT_TARIFF; // revenue (SPD exports)
        double amountOnExport = totalKwhExport * EXPORT_TARIFF; // expense (SPD imports)

        bill.setAmountOnImport(round(amountOnImport));
        bill.setAmountOnExport(round(amountOnExport));

        // Net kWh = revenue - expense
        double netAmountKwh = amountOnImport - amountOnExport;
        bill.setNetAmountKwh(round(netAmountKwh));

        // --- Reactive energy (kVArh) ---
        double kvarhLeadImport = safeDiff(mainMeter.getKvarhLeadImportEnd(), mainMeter.getKvarhLeadImportStart());   // Q1 forward
        double kvarhLagImport  = safeDiff(mainMeter.getKvarhLagImportEnd(),  mainMeter.getKvarhLagImportStart());    // Q3 forward
        double kvarhLeadExport = safeDiff(mainMeter.getKvarhLeadExportEnd(), mainMeter.getKvarhLeadExportStart());   // Q2 reverse
        double kvarhLagExport  = safeDiff(mainMeter.getKvarhLagExportEnd(),  mainMeter.getKvarhLagExportStart());    // Q4 reverse

        bill.setKvarhLeadForward(round(kvarhLeadImport)); // Q1
        bill.setKvarhLagForward(round(kvarhLagImport));   // Q2
        bill.setKvarhLeadReverse(round(kvarhLeadExport)); // Q3
        bill.setKvarhLagReverse(round(kvarhLagExport));   // Q4

        bill.setKvarhTariff(KVARH_TARIFF);

        // Component amounts (positive)
        double amountOnKvarhLeadForward = kvarhLeadImport * KVARH_TARIFF; // Q1
        double amountOnKvarhLagForward  = kvarhLagImport  * KVARH_TARIFF; // Q2
        double amountOnKvarhLeadReverse = kvarhLeadExport * KVARH_TARIFF; // Q3
        double amountOnKvarhLagReverse  = kvarhLagExport  * KVARH_TARIFF; // Q4

        // Net Reactive = (Q1 - Q2 + Q3 - Q4)  ==> (forward - reverse)
        double netReactiveEnergyAmount =
                (amountOnKvarhLagForward + amountOnKvarhLagReverse) - (amountOnKvarhLeadForward + amountOnKvarhLeadReverse);

        bill.setNetAmountKvarh(round(netReactiveEnergyAmount));

        // --- Grand total ---
        double totalPayable = netAmountKwh + netReactiveEnergyAmount;
        bill.setTotalPayableAmount(round(totalPayable));
        bill.setAmount(round(totalPayable)); // sync amount field

        bill.setStatus("PENDING");

        // Invoice number
        String invoiceNo = String.format("INV-%s-%d",
                LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE),
                jmr.getId());
        bill.setInvoiceNumber(invoiceNo);

        return billRepository.save(bill);
    }

    private double safeDiff(Double end, Double start) {
        if (end == null) end = 0.0;
        if (start == null) start = 0.0;
        return end - start;
    }

    private double round(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public Bill markBillAsPaid(Long billId, LocalDate paymentDate, Double actualAmountReceived, Double governmentDeductions) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        bill.setStatus("PAID");
        bill.setPaymentDate(paymentDate);
        bill.setActualAmountReceived(actualAmountReceived);
        bill.setGovernmentDeductions(governmentDeductions);

        // Update income after payment
        incomeService.updateIncomeAfterPayment(bill);

        return billRepository.save(bill);
    }

    public double calculateBalance() {
        Double totalIncome = billRepository.sumActualAmountReceivedByStatus("PAID");
        Double totalGovDeductions = billRepository.sumGovernmentDeductionsByStatus("PAID");

        double income = (totalIncome != null ? totalIncome : 0.0);
        double deductions = (totalGovDeductions != null ? totalGovDeductions : 0.0);
        return round(income - deductions);
    }
}
