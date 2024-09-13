package com.sti.accounting.models;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class LedgerAccountsResponse {

    private Long diaryId;
    private String diaryName;
    private List<TransactionResponse> transactions;

    @Data
    public static class TransactionResponse {
        private Long id;
        private String description;
        private String reference;
        private LocalDateTime creationDate;
        private LocalDate date;
        private List<TransactionDetailResponse> transactionsDetail;
    }

    @Data
    public static class TransactionDetailResponse {
        private Long id;
        private String entryType;
        private String shortEntryType;
        private String accountCode;
        private String accountName;
        private BigDecimal amount;

    }
}
