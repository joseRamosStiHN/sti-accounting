package com.sti.accounting.models;

import com.sti.accounting.utils.Motion;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;


@Getter
@Setter
public class TransactionDetailRequest {

    private Long id;

    private Long accountId;

    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private Motion motion;
}
