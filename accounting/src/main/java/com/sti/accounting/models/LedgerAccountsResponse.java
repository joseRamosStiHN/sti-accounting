package com.sti.accounting.models;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class LedgerAccountsResponse {

    private Long accountId;
    private String accountName;
    private String accountCode;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private BigDecimal balance;
    private List<LedgerAccountDetailResponse> details;
}
