package com.sti.accounting.models;

import lombok.Data;


import java.time.LocalDateTime;
import java.util.Set;

@Data
public class AccountingAdjustmentResponse {

    private Long id;
    private Long transactionId;
    private String reference;
    private String descriptionAdjustment;
    private String invoiceNo;
    private Long diaryType;
    private String diaryName;
    private String numberPda;
    private String status;
    private LocalDateTime creationDate;
    private String user;

    private Set<AdjustmentDetailResponse> adjustmentDetails;

}
