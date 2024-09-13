package com.sti.accounting.models;

import com.sti.accounting.utils.Motion;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdjustmentDetailRequest {

    private Long id;

    private Long accountId;

    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private Motion motion;
}
