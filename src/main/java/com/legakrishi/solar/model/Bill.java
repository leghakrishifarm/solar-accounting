package com.legakrishi.solar.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.Date;

import com.legakrishi.solar.util.NumberToWordsConverter;

@Entity
@Table(name = "bills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String invoiceNumber;

    @Column(nullable = false)
    private String month;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Double amount;  // total payable amount

    @Column(nullable = false)
    private LocalDate billDate;

    @Column(length = 200)
    private String description;

    @Column(nullable = false)
    private String status; // "PENDING" or "PAID"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner_id")
    private Partner partner;

    private Double totalKwhExport;
    private Double totalKwhImport;

    private Double exportTariff;
    private Double importTariff;

    private Double amountOnExport;
    private Double amountOnImport;
    private Double netAmountKwh;

    private Double kvarhLeadForward;
    private Double kvarhLeadReverse;
    private Double kvarhLagForward;
    private Double kvarhLagReverse;

    private Double kvarhTariff;

    private Double netAmountKvarh;  // net reactive energy amount in currency

    @Column(name = "total_payable_amount", nullable = false)
    private Double totalPayableAmount;

    @OneToOne
    @JoinColumn(name = "jmr_report_id")
    private JmrReport jmrReport;

    // New fields for payment tracking
    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "actual_amount_received")
    private Double actualAmountReceived;

    @Column(name = "government_deductions")
    private Double governmentDeductions;

    // Add this field
    @Temporal(TemporalType.DATE)
    private Date readingDate;

   // Computed field for amount in words
    @Transient
    public String getAmountInWords() {
        long rupees = this.amount != null ? this.amount.longValue() : 0;
        return NumberToWordsConverter.convert(rupees);
    }
}
