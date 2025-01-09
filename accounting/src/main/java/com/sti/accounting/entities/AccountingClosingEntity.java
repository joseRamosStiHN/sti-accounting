package com.sti.accounting.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounting_closing")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountingClosingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "accounting_period_id")
    private AccountingPeriodEntity accountingPeriod;

    @Column(name = "START_PERIOD")
    private LocalDateTime startPeriod;

    @Column(name = "END_PERIOD")
    private LocalDateTime endPeriod;

    @Column(name = "TOTAL_ASSETS")
    private BigDecimal totalAssets;

    @Column(name = "TOTAL_LIABILITIES")
    private BigDecimal totalLiabilities;

    @Column(name = "TOTAL_CAPITAL")
    private BigDecimal totalCapital;

    @Column(name = "TOTAL_INCOME")
    private BigDecimal totalIncome;

    @Column(name = "TOTAL_EXPENSES")
    private BigDecimal totalExpenses;

    @Column(name = "NET_INCOME")
    private BigDecimal netIncome;

    @Lob
    @Column(name = "CLOSURE_REPORT_PDF", columnDefinition = "LONGBLOB")
    private byte[] closureReportPdf;

    @Column(name = "TENANT_ID")
    private String tenantId;
}
