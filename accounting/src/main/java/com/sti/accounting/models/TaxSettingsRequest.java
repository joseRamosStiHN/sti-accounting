package com.sti.accounting.models;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaxSettingsRequest {

    @NotNull(message = "Tax Rate is required")
    private String taxRate;

    @NotBlank(message = "Type is required")
    private String type;

    @NotNull(message = "From is required")
    private BigDecimal from;

    @NotNull(message = "To is required")
    private BigDecimal to;

    private Boolean isCurrent;
}
