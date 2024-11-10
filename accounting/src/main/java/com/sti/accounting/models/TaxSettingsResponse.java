package com.sti.accounting.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaxSettingsResponse {

    private String taxRate;

    private String type;

    private BigDecimal from;

    private BigDecimal to;

    private Boolean isCurrent;

    private LocalDateTime creationDate;

}
