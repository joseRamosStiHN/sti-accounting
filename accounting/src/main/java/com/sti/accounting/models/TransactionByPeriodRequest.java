package com.sti.accounting.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
public class TransactionByPeriodRequest {

    @NotBlank(message = "account is required")
    private String account;


    @NotNull(message = "Date is required")
    private LocalDate initDate;

    @NotNull(message = "Date is required")
    private LocalDate endDate;

}


