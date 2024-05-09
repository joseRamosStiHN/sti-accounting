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


    private Integer id;

    private String accountName;

    private Integer parentId;

    private String category;

    private boolean isRoot;

    private BigDecimal amount;



}
