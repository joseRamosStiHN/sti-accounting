package com.sti.accounting.models;

import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdjustmentDetailRequest {

    private Long id;

    private Long accountId;

    @Positive(message = "Debit must be positive")
    private BigDecimal debit;

    @Positive(message = "Credit must be positive")
    private BigDecimal credit;

}
