package com.sti.accounting.models;

import lombok.Data;

import java.util.List;
@Data
public class BulkTransactionResponse {


    private  Long id;

    private String name;

    private boolean status;

    private Long type;

    private int rowInit;

    private String tenantId;

    private List<BulkTransactionRequestList> configDetails;


}
