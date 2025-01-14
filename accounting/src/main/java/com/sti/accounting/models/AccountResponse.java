package com.sti.accounting.models;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class AccountResponse {
    private Long id;
    private String name;
    private String accountCode;
    private String parentName;
    private Long parentId;
    private String parentCode;
    private String categoryName;
    private Long categoryId;
    private String typicallyBalance;
    private String accountTypeName;
    private Long accountType;
    private String status;
    private Boolean supportEntry;
    private Boolean asTransaction;
    private boolean hasChildAccounts;

    private Set<AccountBalance> balances = new HashSet<>();
}
