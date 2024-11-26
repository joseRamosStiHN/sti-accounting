package com.sti.accounting.models;

import lombok.Data;

import java.util.List;

@Data
public class BulkTransactionRequest {

    private String name;

    private Long type;

    private int rowInit;

    private List<BulkTransactionRequestList> configDetails;



}
