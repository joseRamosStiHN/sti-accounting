package com.sti.accounting.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeniorAccountsTransactionResponse {

    private  String name;
    private  String movimiento;
    private  BigDecimal debe;
    private BigDecimal haber;
    private BigDecimal balance;
    private Long numberPda;


}
