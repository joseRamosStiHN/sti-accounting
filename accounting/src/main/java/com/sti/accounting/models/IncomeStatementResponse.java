package com.sti.accounting.models;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class IncomeStatementResponse {
    private List<Transaction> transactions;

    @Data
    public static class Transaction {
        private Long id;
        private String category;
        private String accountParent;
        private String account;
        private BigDecimal amount;
        private Date date;
    }
}


