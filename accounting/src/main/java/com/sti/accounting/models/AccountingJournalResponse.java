package com.sti.accounting.models;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AccountingJournalResponse {

    private Long id;

    private String diaryName;

    private String accountTypeName;

    private Long accountType;

    private Long defaultIncomeAccount;

    private String defaultIncomeAccountName;

    private String defaultIncomeAccountCode;

    private Long defaultExpenseAccount;

    private String defaultExpenseAccountName;

    private String defaultExpenseAccountCode;

    private Long cashAccount;

    private String cashAccountName;

    private String cashAccountCode;

    private Long lossAccount;

    private String lossAccountName;

    private String lossAccountCode;

    private Long transitAccount;

    private String transitAccountName;

    private String transitAccountCode;

    private Long profitAccount;

    private String profitAccountName;

    private String profitAccountCode;

    private Long bankAccount;

    private String bankAccountName;

    private String bankAccountCode;

    private BigDecimal accountNumber;

    private Long defaultAccount;

    private String defaultAccountName;

    private String defaultAccountCode;

    private String code;

    private boolean status;

    private LocalDateTime createDate;

}
