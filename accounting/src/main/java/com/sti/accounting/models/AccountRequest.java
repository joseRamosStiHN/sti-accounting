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

    @NotBlank(message = "Financial Statement Type is required")
    private String financialStatementType;

    @DecimalMin(value = "0", inclusive = false, message = "Parent ID must be greater than 0")
    private BigDecimal parentId;

    @NotBlank(message = "Currency is required")
    private String currency;

    @DecimalMin(value = "0", inclusive = false, message = "Category must be greater than 0")
    private BigDecimal category;

    @DecimalMin(value = "0", inclusive = false, message = "Account Type must be greater than 0")
    private BigDecimal accountType;

    @NotBlank(message = "Typical Balance is required")
    private String typicalBalance;

    private boolean supportsRegistration;

    @PositiveOrZero(message = "Initial Balance must be positive or 0")
    private BigDecimal initialBalance;

    private List<BalancesEntity> balances;
}
