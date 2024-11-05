package com.sti.accounting.models;

import com.sti.accounting.utils.Currency;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.constraints.*;

@Data
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

    private String typeSale;

    private BigDecimal cashValue;

    private BigDecimal creditValue;

    private String typePayment;

    private String rtn;

    private String supplierName;

    @NotNull(message = "Diary Type is required")
    private Long diaryType;

    private List<TransactionDetailRequest> detail;
}


