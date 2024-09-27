package com.sti.accounting.models;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdjustmentDetailResponse {

    private Long id;
    private Long accountId;
    private String accountName;
    private String accountCode;
    private BigDecimal debit;
    private BigDecimal credit;
    private String typicalBalance;
    private BigDecimal initialBalance;

}
