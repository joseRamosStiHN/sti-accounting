package com.sti.accounting.models;

import com.sti.accounting.utils.Status;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import jakarta.validation.constraints.*;

@Getter
@Setter
public class AccountRequest {

    private Long id;

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Description is required")
    private String description;

    private Long parentId;

    @NotNull(message = "Category is required")
    @DecimalMin(value = "0", inclusive = false, message = "Category must be greater than 0")
    private BigDecimal category;

    @NotBlank(message = "Typical Balance is required")
    private String typicalBalance;

    @NotNull(message = "Account Type is required")
    @DecimalMin(value = "0", inclusive = false, message = "Account Type must be greater than 0")
    private BigDecimal accountType;

    private boolean supportsRegistration;

    private Status status;

    private Set<AccountBalance> balances = new HashSet<>();

    @AssertTrue(message = "Parent Id is required")
    private boolean isValidParentId() {
        return !supportsRegistration || parentId != null;
    }
}
