package com.sti.accounting.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeniorAccountsResponse {


    private String name;
    private String code;
    private String fatherAccount;
    private String typeAccount;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private BigDecimal balance;

    private List<SeniorAccountsTransactionResponse> transaction;


}
