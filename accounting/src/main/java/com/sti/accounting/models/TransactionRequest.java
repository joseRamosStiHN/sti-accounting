package com.sti.accounting.models;

import com.sti.accounting.utils.Currency;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.constraints.*;

@Getter
@Setter
public class TransactionRequest {

    private Long id;

    private LocalDateTime createAtTime;

    @NotNull(message = "Date is required")
    private LocalDate createAtDate;

    private Long status;

    @NotBlank(message = "Reference is required")
    private String reference;

    @NotNull(message = "Document Type is required")
    private BigInteger documentType;

    @NotNull(message = "Exchange Rate is required")
    @Positive(message = "Exchange Rate must be positive")
    private BigDecimal exchangeRate;

    @NotNull(message = "Description Pda is required")
    private String descriptionPda;

    @NotNull(message = "Number Pda is required")
    @Positive(message = "Number Pda must be positive")
    private BigInteger numberPda;

    private Currency currency;

    private List<TransactionDetailRequest> detail;
}


