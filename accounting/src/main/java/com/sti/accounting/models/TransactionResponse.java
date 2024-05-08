package com.sti.accounting.models;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Data
public class TransactionResponse {
    private Long id;
    private LocalDate date;
    private String currency;
    private String description;
    private Long documentType;
    private String documentName;
    private String reference;
    private String entryNumber;
    private String status;
    private BigDecimal exchangeRate;

    private Set<TransactionDetailResponse> transactionDetails;

}
