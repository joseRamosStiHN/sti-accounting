package com.sti.accounting.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrialBalanceResponse {

    private List<PeriodBalanceResponse> periods;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PeriodBalanceResponse {
        private Long id;
        private String periodName;
        private String closureType;
        private LocalDateTime startPeriod;
        private LocalDateTime endPeriod;
        private boolean status;
        private List<AccountBalance> accountBalances;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AccountBalance {

        private Long id;
        private String name;
        private String accountCode;
        private String parentName;
        private Long parentId;
        List<InitialBalance> initialBalance;
        List<BalancePeriod> balancePeriod;
        List<FinalBalance> finalBalance;

    }
    //TODO: hay que revisar esto Laurent
    @EqualsAndHashCode(callSuper = true)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InitialBalance extends Balance{
        private BigDecimal debit;
        private BigDecimal credit;

    }

    @EqualsAndHashCode(callSuper = true)
    @Data
   // @AllArgsConstructor
   // @NoArgsConstructor
    public static class BalancePeriod extends Balance{
       // private BigDecimal debit;
       // private BigDecimal credit;

    }

    @EqualsAndHashCode(callSuper = true)
    @Data
   // @AllArgsConstructor
   // @NoArgsConstructor
    public static class FinalBalance extends Balance{
        //private BigDecimal debit;
       // private BigDecimal credit;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Balance {
        private BigDecimal debit;
        private BigDecimal credit;

    }



}
