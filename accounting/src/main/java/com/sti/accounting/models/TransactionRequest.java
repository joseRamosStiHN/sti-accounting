package com.sti.accounting.models;

import com.sti.accounting.utils.Currency;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.*;

@Getter
@Setter
public class TransactionRequest {

    private Long id;

    @NotNull(message = "Date is required")
    private LocalDate createAtDate;

    private Long status;

    @NotBlank(message = "Reference is required")
    private String reference;

    @NotNull(message = "Document Type is required")
    private Long documentType;

    @NotNull(message = "Exchange Rate is required")
    private BigDecimal exchangeRate;

    @NotNull(message = "Description Pda is required")
    private String descriptionPda;

    private Currency currency;

    private List<TransactionDetailRequest> detail;
}


