package com.sti.accounting.models;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LedgerAccountsResponse {

    private Long parentAccountId;
    private String parentAccountName;
    private String parentAccountCode;
    private List<ChildAccountResponse> childAccounts;

    @Data
    public static class ChildAccountResponse {
        private Long accountId;
        private String accountName;
        private String accountCode;
        private List<TransactionDetailResponse> transactions;
    }

    @Data
    public static class TransactionDetailResponse {
        private Long id;
        private String description;
        private String entryType;
        private String shortEntryType;
        private String accountCode;
        private String accountName;
        private BigDecimal amount;
        private LocalDateTime creationDate;
        private LocalDate date;

    }
}
