package com.sti.accounting.models;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionDetailResponse {
    private Long id;
    private BigDecimal amount;
    private String entryType;
    private String shortEntryType;
    private String accountName;
    private String accountCode;
    private Long accountId;
}
