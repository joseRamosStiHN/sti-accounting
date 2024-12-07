package com.sti.accounting.models;

import com.sti.accounting.utils.PeriodStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountingPeriodRequest {

    private Long id;

    private String periodName;

    @NotBlank(message = "Closure type is required.")
    private String closureType;

    @NotNull(message = "Start period is required.")
    private LocalDateTime startPeriod;

    private LocalDateTime endPeriod;

    private Integer daysPeriod;

    private PeriodStatus periodStatus;

    private Integer periodOrder;

    private Boolean isAnnual;



}
