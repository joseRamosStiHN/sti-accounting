package com.sti.accounting.models;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountBalance {
    private BigDecimal initialBalance;
    private Boolean isCurrent;
}
