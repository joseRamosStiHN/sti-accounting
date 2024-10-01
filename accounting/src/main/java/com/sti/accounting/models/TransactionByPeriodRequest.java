package com.sti.accounting.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class TransactionByPeriodRequest {

    @NotBlank(message = "account is required")
    private String account;


    @NotNull(message = "Date is required")
    private LocalDate initDate;

    @NotNull(message = "Date is required")
    private LocalDate endDate;

}


