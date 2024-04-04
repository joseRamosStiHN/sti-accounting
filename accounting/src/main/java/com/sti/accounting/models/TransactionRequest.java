package com.sti.accounting.models;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.constraints.*;

@Getter
@Setter
public class TransactionRequest {

    private Long id;

    private LocalDateTime createAtTime;

    private LocalDate createAtDate;

    private Long status;

    @NotBlank(message = "Reference is required")
    private String reference;

    @NotNull(message = "Document Type is required")
    private Long documentType;

    @NotNull(message = "Exchange Rate is required")
    @Positive(message = "Exchange Rate must be positive")
    private BigDecimal exchangeRate;

    private List<TransactionDetailRequest> detail;
}


