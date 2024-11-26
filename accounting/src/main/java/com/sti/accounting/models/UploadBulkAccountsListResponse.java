package com.sti.accounting.models;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UploadBulkAccountsListResponse {

    private String title;

    private Long account;

    private BigDecimal debit;

    private BigDecimal credit;
}
