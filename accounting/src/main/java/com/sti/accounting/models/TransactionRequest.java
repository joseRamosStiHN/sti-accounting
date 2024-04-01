package com.sti.accounting.models;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class TransactionRequest {

    private String reference;
    private Long documentType;
    private BigDecimal exchangeRate;
    private List<TransactionDetailRequest> detail;
}


