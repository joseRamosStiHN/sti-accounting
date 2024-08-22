package com.sti.accounting.models;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.*;

@Getter
@Setter
public class BalancesRequest {

    private Long id;

    private Long accountId;

    @NotBlank(message = "Typical Balance is required")
    private String typicalBalance;

    private BigDecimal initialBalance;

    private LocalDateTime createAtDate;

    private Boolean isActual;
}
