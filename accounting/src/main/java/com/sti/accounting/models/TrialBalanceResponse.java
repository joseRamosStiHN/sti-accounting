package com.sti.accounting.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TrialBalanceResponse {

    private Long id;
    private String periodName;
    private String closureType;
    private LocalDateTime startPeriod;
    private LocalDateTime endPeriod;
    List<BalanceDiary> balanceDiaries;

    public TrialBalanceResponse(List<InitialBalance> initialBalances, List<BalancePeriod> balancePeriods, List<FinalBalance> finalBalances) {
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BalanceDiary {
        private String diaryName;
        List<InitialBalance> initialBalance;
        List<BalancePeriod> balancePeriod;
        List<FinalBalance> finalBalance;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class InitialBalance {
        private BigDecimal debit;
        private BigDecimal credit;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BalancePeriod {
        private BigDecimal debit;
        private BigDecimal credit;

    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FinalBalance {
        private BigDecimal debit;
        private BigDecimal credit;

    }

}
