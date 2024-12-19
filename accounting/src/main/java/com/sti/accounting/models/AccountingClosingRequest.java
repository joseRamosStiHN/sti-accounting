package com.sti.accounting.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountingClosingRequest {

    private Long id;

    private LocalDateTime startPeriod;

    private LocalDateTime endPeriod;

    private BigDecimal totalAssets;

    private BigDecimal totalLiabilities;

    private BigDecimal totalCapital;

    private BigDecimal totalIncome;

    private BigDecimal totalExpenses;

    private BigDecimal netIncome;

}
