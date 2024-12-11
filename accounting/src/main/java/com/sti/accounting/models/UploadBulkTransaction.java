package com.sti.accounting.models;

import com.sti.accounting.utils.Status;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class UploadBulkTransaction {

    private Integer row;

    private String date;
    private String currency;
    private String description;

    private String errors;
    private String reference;
    private Status status;
    private BigDecimal exchangeRate;
    private String typeSale;
    private String typePayment;
    private String rtn;
    private String supplierName;


    private List<UploadBulkAccountsListResponse> accounts;

    private List<UploadBulkOthersFieldsList> otherFields;
}
