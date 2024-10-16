package com.sti.accounting.models;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class IncomeStatementResponse {

    private Long id;
    private String category;
    private String accountParent;
    private String typicalBalance;
    private String account;
    private BigDecimal amount;
    private Date date;

}


