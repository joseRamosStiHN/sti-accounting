package com.sti.accounting.models;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BalanceGeneralResponse {

    private List<GeneralBalanceResponse> assets;
    private List<GeneralBalanceResponse> liabilities;
    private List<GeneralBalanceResponse> equity;
}