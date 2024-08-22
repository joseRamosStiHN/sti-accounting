package com.sti.accounting.models;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BalancesResponse {

    private Long id;
    private Long accountId;
    private String typicalBalance;
    private BigDecimal initialBalance;
    private LocalDateTime createAtDate;
    private Boolean isActual;

}
