package com.sti.accounting.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountSummaryResponse {

    private String accountName;
    private BigDecimal totalDebit ;
    private BigDecimal totalCredit ;
    private BigDecimal balance;
    private Set<Details> details;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Details {

        private LocalDateTime creationDate;
        private LocalDate date;
        private String description;
        private String numberPda;
        private String categoryName;
        private Long accountId;
        private String accountName;
        private String accountCode;
        private BigDecimal amount;
        private String entryType;
        private String shortEntryType;
    }
}
