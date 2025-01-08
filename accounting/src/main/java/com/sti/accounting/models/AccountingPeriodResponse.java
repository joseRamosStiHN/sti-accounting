package com.sti.accounting.models;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccountingPeriodResponse {

    private Long id;
    private String periodName;
    private String closureType;
    private LocalDateTime startPeriod;
    private LocalDateTime endPeriod;
    private Integer daysPeriod;
    private String periodStatus;
    private Integer periodOrder;
    private Boolean isAnnual;
    private String tenantId;

}
