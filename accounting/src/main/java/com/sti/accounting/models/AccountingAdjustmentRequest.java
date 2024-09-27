package com.sti.accounting.models;

import lombok.Data;

import java.util.List;


@Data
public class AccountingAdjustmentRequest {

    private Long id;

    private Long transactionId;

    private String reference;

    private String descriptionAdjustment;

    private Long status;

    private List<AdjustmentDetailRequest> detailAdjustment;

}
