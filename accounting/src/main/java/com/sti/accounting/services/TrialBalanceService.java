package com.sti.accounting.services;

import com.sti.accounting.entities.AccountingPeriodEntity;
import com.sti.accounting.models.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;


@Service
public class TrialBalanceService {

    private final TransactionService transactionService;
    private final AccountingPeriodService accountingPeriodService;
    private final AccountingAdjustmentService accountingAdjustmentService;
    private final CreditNotesService creditNotesService;
    private final DebitNotesService debitNotesService;

    public TrialBalanceService(TransactionService transactionService, AccountingPeriodService accountingPeriodService, AccountingAdjustmentService accountingAdjustmentService, CreditNotesService creditNotesService, DebitNotesService debitNotesService) {
        this.transactionService = transactionService;
        this.accountingPeriodService = accountingPeriodService;
        this.accountingAdjustmentService = accountingAdjustmentService;
        this.creditNotesService = creditNotesService;
        this.debitNotesService = debitNotesService;
    }

    public TrialBalanceResponse getTrialBalance() {
        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        // Seteo de valores del periodo activo
        TrialBalanceResponse trialBalanceResponse = new TrialBalanceResponse();
        trialBalanceResponse.setId(activePeriod.getId());
        trialBalanceResponse.setPeriodName(activePeriod.getPeriodName());
        trialBalanceResponse.setClosureType(activePeriod.getClosureType());
        trialBalanceResponse.setStartPeriod(activePeriod.getStartPeriod());
        trialBalanceResponse.setEndPeriod(activePeriod.getEndPeriod());

        // Obtener transacciones y ajustes para el periodo activo
        //Utilizando el servicio de asientos y apuntes que trae todas las transacciones y ajustes
        SeniorAccountantsResponse transactionAdjustment = new SeniorAccountantsService(accountingAdjustmentService, transactionService, accountingPeriodService, creditNotesService, debitNotesService).getSeniorAccountants();

        // Agregar balance diario al response
        List<TrialBalanceResponse.BalanceDiary> balanceDiaries = calculateInitialBalance(transactionAdjustment);

        trialBalanceResponse.setBalanceDiaries(balanceDiaries);

        // Agregar el balance del periodo y el valance final para cada diario
        trialBalanceResponse.getBalanceDiaries().forEach(balanceDiary -> {
            TrialBalanceResponse.BalancePeriod balancePeriod = calculateBalancePeriod(balanceDiary.getDiaryName(), transactionAdjustment);
            balanceDiary.setBalancePeriod(Collections.singletonList(balancePeriod));

            TrialBalanceResponse.FinalBalance finalBalance = calculateFinalBalance(balancePeriod, balanceDiary);
            balanceDiary.setFinalBalance(Collections.singletonList(finalBalance));
        });

        trialBalanceResponse.setBalanceDiaries(balanceDiaries);

        return trialBalanceResponse;
    }

    private List<TrialBalanceResponse.BalanceDiary> calculateInitialBalance(SeniorAccountantsResponse transactionAdjustment) {
        Map<String, Map<String, BigDecimal>> balanceMap = new HashMap<>();

        // Procesar transacciones
        transactionAdjustment.getTransactions().forEach(transaction -> {
            String diaryName = transaction.getDiaryName();

            if (!balanceMap.containsKey(diaryName)) {
                balanceMap.put(diaryName, new HashMap<>());
            }

            Map<String, BigDecimal> diaryBalanceMap = balanceMap.get(diaryName);

            transaction.getTransactionDetails().forEach(detail -> {
                String accountName = detail.getAccountName();
                BigDecimal initialBalance = detail.getInitialBalance();

                if (!diaryBalanceMap.containsKey(accountName)) {
                    diaryBalanceMap.put(accountName, initialBalance);
                }
            });
        });

        // Procesar ajustes
        transactionAdjustment.getAdjustments().forEach(adjustment -> {
            String diaryName = adjustment.getDiaryName();

            if (!balanceMap.containsKey(diaryName)) {
                balanceMap.put(diaryName, new HashMap<>());
            }

            Map<String, BigDecimal> diaryBalanceMap = balanceMap.get(diaryName);

            adjustment.getAdjustmentDetails().forEach(detail -> {
                String accountName = detail.getAccountName();
                BigDecimal initialBalance = detail.getInitialBalance();

                if (!diaryBalanceMap.containsKey(accountName)) {
                    diaryBalanceMap.put(accountName, initialBalance);
                }
            });
        });

        // Procesar Debit Notes
        transactionAdjustment.getDebitNotes().forEach(debitNote -> {
            String diaryName = debitNote.getDiaryName();

            if (!balanceMap.containsKey(diaryName)) {
                balanceMap.put(diaryName, new HashMap<>());
            }

            Map<String, BigDecimal> diaryBalanceMap = balanceMap.get(diaryName);

            debitNote.getDetailNote().forEach(detail -> {
                String accountName = detail.getAccountName();
                BigDecimal initialBalance = detail.getInitialBalance();

                if (!diaryBalanceMap.containsKey(accountName)) {
                    diaryBalanceMap.put(accountName, initialBalance);
                }
            });
        });

        // Procesar Credit Notes
        transactionAdjustment.getCreditNotes().forEach(creditNote -> {
            String diaryName = creditNote.getDiaryName();

            if (!balanceMap.containsKey(diaryName)) {
                balanceMap.put(diaryName, new HashMap<>());
            }

            Map<String, BigDecimal> diaryBalanceMap = balanceMap.get(diaryName);

            creditNote.getDetailNote().forEach(detail -> {
                String accountName = detail.getAccountName();
                BigDecimal initialBalance = detail.getInitialBalance();

                if (!diaryBalanceMap.containsKey(accountName)) {
                    diaryBalanceMap.put(accountName, initialBalance);
                }
            });
        });


        // Crear BalanceDiary para cada diario
        List<TrialBalanceResponse.BalanceDiary> balanceDiaries = new ArrayList<>();

        balanceMap.forEach((diaryName, diaryBalanceMap) -> {
            TrialBalanceResponse.BalanceDiary balanceDiary = new TrialBalanceResponse.BalanceDiary();
            balanceDiary.setDiaryName(diaryName);

            final BigDecimal[] totalCredit = new BigDecimal[]{BigDecimal.ZERO};
            final BigDecimal[] totalDebit = new BigDecimal[]{BigDecimal.ZERO};

            diaryBalanceMap.forEach((accountName, initialBalance) -> {
                if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
                    totalCredit[0] = totalCredit[0].add(initialBalance);
                } else {
                    totalDebit[0] = totalDebit[0].add(initialBalance);
                }
            });

            TrialBalanceResponse.InitialBalance initialBalance = new TrialBalanceResponse.InitialBalance();
            initialBalance.setDebit(totalDebit[0]);
            initialBalance.setCredit(totalCredit[0]);
            balanceDiary.setInitialBalance(Collections.singletonList(initialBalance));

            balanceDiaries.add(balanceDiary);
        });

        return balanceDiaries;
    }

    private TrialBalanceResponse.BalancePeriod calculateBalancePeriod(String diaryName, SeniorAccountantsResponse transactionAdjustment) {
        final BigDecimal[] balancePeriodCredit = new BigDecimal[]{BigDecimal.ZERO};
        final BigDecimal[] balancePeriodDebit = new BigDecimal[]{BigDecimal.ZERO};

        transactionAdjustment.getTransactions().forEach(transaction -> {
            if (transaction.getDiaryName().equals(diaryName)) {
                transaction.getTransactionDetails().forEach(detail -> {
                    if (detail.getShortEntryType().equals("C")) {
                        balancePeriodCredit[0] = balancePeriodCredit[0].add(detail.getAmount());
                    } else {
                        balancePeriodDebit[0] = balancePeriodDebit[0].add(detail.getAmount());
                    }
                });
            }
        });

        transactionAdjustment.getAdjustments().forEach(adjustment -> {
            if (adjustment.getDiaryName().equals(diaryName)) {
                adjustment.getAdjustmentDetails().forEach(detail -> {
                    if (detail.getShortEntryType().equals("C")) {
                        balancePeriodCredit[0] = balancePeriodCredit[0].add(detail.getAmount());
                    } else {
                        balancePeriodDebit[0] = balancePeriodDebit[0].add(detail.getAmount());
                    }
                });
            }
        });

        transactionAdjustment.getDebitNotes().forEach(debitNotes -> {
            if (debitNotes.getDiaryName().equals(diaryName)) {
                debitNotes.getDetailNote().forEach(detail -> {
                    if (detail.getShortEntryType().equals("C")) {
                        balancePeriodCredit[0] = balancePeriodCredit[0].add(detail.getAmount());
                    } else {
                        balancePeriodDebit[0] = balancePeriodDebit[0].add(detail.getAmount());
                    }
                });
            }
        });
        transactionAdjustment.getCreditNotes().forEach(creditNotes -> {
            if (creditNotes.getDiaryName().equals(diaryName)) {
                creditNotes.getDetailNote().forEach(detail -> {
                    if (detail.getShortEntryType().equals("C")) {
                        balancePeriodCredit[0] = balancePeriodCredit[0].add(detail.getAmount());
                    } else {
                        balancePeriodDebit[0] = balancePeriodDebit[0].add(detail.getAmount());
                    }
                });
            }
        });


        TrialBalanceResponse.BalancePeriod balancePeriod = new TrialBalanceResponse.BalancePeriod();
        balancePeriod.setCredit(balancePeriodCredit[0]);
        balancePeriod.setDebit(balancePeriodDebit[0]);

        return balancePeriod;
    }

    private TrialBalanceResponse.FinalBalance calculateFinalBalance(TrialBalanceResponse.BalancePeriod balancePeriod, TrialBalanceResponse.BalanceDiary balanceDiary) {
        TrialBalanceResponse.FinalBalance finalBalance = new TrialBalanceResponse.FinalBalance();

        BigDecimal debit = balancePeriod.getDebit();
        BigDecimal credit = balancePeriod.getCredit();

        // Sumar el valor del initialBalance al balancePeriod
        if (balanceDiary.getInitialBalance().getFirst().getCredit().compareTo(BigDecimal.ZERO) > 0) {
            credit = credit.add(balanceDiary.getInitialBalance().getFirst().getCredit());
        } else {
            debit = debit.add(balanceDiary.getInitialBalance().getFirst().getDebit());
        }

        if (debit.compareTo(credit) > 0) {
            finalBalance.setDebit(debit.subtract(credit));
            finalBalance.setCredit(BigDecimal.ZERO);
        } else {
            finalBalance.setDebit(BigDecimal.ZERO);
            finalBalance.setCredit(credit.subtract(debit));
        }

        return finalBalance;
    }
}
