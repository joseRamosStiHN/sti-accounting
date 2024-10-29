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
    private String cuentaPadre;
    private String tipoCuenta;
    private BigDecimal totalDebe;
    private BigDecimal totalHaber;
    private BigDecimal balance;


    private List<SeniorAccountsTransactionResponse> transaction;




}
