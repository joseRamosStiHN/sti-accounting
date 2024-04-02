package com.sti.accounting.models;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class BalancesRequest {

    private Long id;

    private Long accountId;

    @PositiveOrZero(message = "Initial Balance must be positive or 0")
    private BigDecimal initialBalance;

    private LocalDateTime createAtDate;

    private Boolean isActual;
}
