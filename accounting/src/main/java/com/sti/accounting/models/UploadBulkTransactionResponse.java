package com.sti.accounting.models;

import lombok.Data;
import java.util.List;

@Data
public class UploadBulkTransactionResponse {


    private Long typeTransaction;

    private List<UploadBulkTransaction> data;

    private  List<UploadBulkTransaction> errors;


}
