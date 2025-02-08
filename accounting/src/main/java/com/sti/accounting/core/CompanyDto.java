package com.sti.accounting.core;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CompanyDto {

    private Long id;

    @NotNull
    private String name;
    private String description;
    private String address;

    @NotNull
    private String rtn;

    @NotNull
    private String type;
    private String email;
    private String phone;
    private String website;
    private boolean isActive;
    private String tenantId;
    private LocalDate createdAt;
}
