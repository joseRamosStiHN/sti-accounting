package com.sti.accounting.models;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BalanceGeneralResponse {


    private Long id;

    private String accountName;

    private Long parentId;

    private String category;

    private boolean isRoot;

    private BigDecimal amount;



}
