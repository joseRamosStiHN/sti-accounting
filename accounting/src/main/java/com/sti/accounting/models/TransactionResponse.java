package com.sti.accounting.models;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class TransactionResponse {
    private Long id;
    private LocalDateTime creationDate;
    private LocalDate date;
    private String currency;
    private String description;
    private Long documentType;
    private String documentName;
    private String reference;
    private Long diaryType;
    private String diaryName;
    private String numberPda;
    private String status;
    private BigDecimal exchangeRate;

    private Set<TransactionDetailResponse> transactionDetails;

}
