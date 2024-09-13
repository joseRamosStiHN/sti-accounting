package com.sti.accounting.models;

import lombok.Data;


import java.time.LocalDateTime;
import java.util.Set;

@Data
public class AccountingAdjustmentResponse {

    private Long id;
    private Long transactionId;
    private String reference;
    private String invoiceNo;
    private String status;
    private LocalDateTime creationDate;

    private Set<AdjustmentDetailResponse> adjustmentDetails;

}
