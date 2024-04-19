package com.sti.accounting.models;

import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.utils.Status;
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

    private BigDecimal parentId;

    @NotNull(message = "Category is required")
    @DecimalMin(value = "0", inclusive = false, message = "Category must be greater than 0")
    private BigDecimal category;

    @NotBlank(message = "Typical Balance is required")
    private String typicalBalance;

    private boolean supportsRegistration;

    private Status status;

    private List<BalancesEntity> balances;

    @AssertTrue(message = "Parent Id is required")
    private boolean isValidParentId() {
        return !supportsRegistration || parentId != null;
    }
}
