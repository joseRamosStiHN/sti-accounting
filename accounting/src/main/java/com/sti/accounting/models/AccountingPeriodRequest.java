package com.sti.accounting.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AccountingPeriodRequest {

    private Long id;

    private String periodName;

    @NotBlank(message = "Closure type is required.")
    private String closureType;

    @NotNull(message = "Start period is required.")
    private LocalDateTime startPeriod;

    private LocalDateTime endPeriod;

    private Integer daysPeriod;

    private boolean status;


}
