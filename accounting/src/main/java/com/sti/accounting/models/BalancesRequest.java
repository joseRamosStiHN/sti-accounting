package com.sti.accounting.models;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

@Getter
@Setter
public class BalancesRequest {

    private Long id;

    private Long accountId;

    @NotNull(message = "Initial Balance is required")
    @Positive(message = "Initial Balance must be positive")
    private BigDecimal initialBalance;

    private LocalDateTime createAtDate;

    private Boolean isActual;
}
