package com.sti.accounting.models;

import com.sti.accounting.utils.Status;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UploadBulkTransactionResponse {


    private Long typetransaction;

    private List<UploadBulkTransaction> data;

    private  List<UploadBulkTransaction> errors;


}
