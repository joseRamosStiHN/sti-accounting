package com.sti.accounting.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AccountingJournalRequest {

    private Long id;

    @NotBlank(message = "Diary Name is required")
    private String diaryName;

    @NotNull(message = "Account Type is required")
    private BigDecimal accountType;

    private Long defaultIncomeAccount;

    private Long defaultExpenseAccount;

    private Long cashAccount;

    private Long lossAccount;

    private Long transitAccount;

    private Long profitAccount;

    private Long bankAccount;

    private BigDecimal accountNumber;

    private Long defaultAccount;

    private String code;

    private boolean status;

    private LocalDateTime createDate;
}
