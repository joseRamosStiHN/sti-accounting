package com.sti.accounting.models;

import com.sti.accounting.entities.BalancesEntity;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.*;

@Getter
@Setter
public class AccountRequest {

    private Long id;

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Parent Id is required")
    @DecimalMin(value = "0", inclusive = false, message = "Parent ID must be greater than 0")
    private BigDecimal parentId;

    @NotNull(message = "Category is required")
    @DecimalMin(value = "0", inclusive = false, message = "Category must be greater than 0")
    private BigDecimal category;

    @NotBlank(message = "Typical Balance is required")
    private String typicalBalance;

    private boolean supportsRegistration;

    private List<BalancesEntity> balances;
}
