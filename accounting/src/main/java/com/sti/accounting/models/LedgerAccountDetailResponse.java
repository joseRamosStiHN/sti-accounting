package com.sti.accounting.models;

import lombok.Data;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

@Data
public class LedgerAccountDetailResponse {

    private Long transactionId;
    private BigInteger numberPda;
    private Long accountId;
    private String accountCode;
    private String accountName;
    private String accountTypeName;
    private Long accountType;
    private BigDecimal debit;
    private BigDecimal credit;
    private LocalDate date;
}
