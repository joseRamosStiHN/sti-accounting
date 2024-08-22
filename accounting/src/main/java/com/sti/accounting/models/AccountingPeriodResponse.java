package com.sti.accounting.models;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AccountingPeriodResponse {

    private Long id;
    private String description;
    private String closureType;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String status;
}
