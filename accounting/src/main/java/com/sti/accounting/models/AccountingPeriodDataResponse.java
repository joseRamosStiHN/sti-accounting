package com.sti.accounting.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AccountingPeriodDataResponse {

    private List<TransactionResponse> transactions;
    private List<AccountingAdjustmentResponse> adjustments;

}
