package com.sti.accounting.models;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CloneAccountDTO {

    private Long id;
    private String code;
    private String description;
    private String typicalBalance;
    private boolean supportsRegistration;
    private String status;
    private Long parentId;
    private AccountCategory accountCategory;
    private AccountType accountType;
    private String tenantId;
    private List<CloneAccountDTO> children;
    private String balances;
}