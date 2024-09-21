package com.sti.accounting.models;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class GeneralBalanceResponse {

    private Long accountId;
    private String accountName;
    private Long parentId;
    private String category;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal balance;
}
