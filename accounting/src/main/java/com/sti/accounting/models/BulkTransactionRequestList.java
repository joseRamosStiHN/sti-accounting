package com.sti.accounting.models;

import com.sti.accounting.utils.BulkDetailType;
import lombok.Data;

@Data
public class BulkTransactionRequestList {


    private Integer colum;

    private String title;

    private Long account;

    private String operation;

    private BulkDetailType bulkTypeData;

    private String field;



}
