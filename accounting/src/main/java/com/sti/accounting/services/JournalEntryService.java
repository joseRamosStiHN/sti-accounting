package com.sti.accounting.services;

import com.sti.accounting.entities.AccountingPeriodEntity;
import com.sti.accounting.models.AccountingAdjustmentResponse;
import com.sti.accounting.models.AccountingPeriodDataResponse;
import com.sti.accounting.models.TransactionResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JournalEntryService {

    private final AccountingAdjustmentService accountingAdjustmentService;
    private final TransactionService transactionService;
    private final AccountingPeriodService accountingPeriodService;

    public JournalEntryService(AccountingAdjustmentService accountingAdjustmentService, TransactionService transactionService, AccountingPeriodService accountingPeriodService) {
        this.accountingAdjustmentService = accountingAdjustmentService;
        this.transactionService = transactionService;
        this.accountingPeriodService = accountingPeriodService;
    }


    public AccountingPeriodDataResponse getTransactionAdjustment() {
        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        LocalDate startDate = activePeriod.getStartPeriod().toLocalDate();
        LocalDate endDate = activePeriod.getEndPeriod().toLocalDate();

        // Obtener transacciones y ajustes para el periodo activo
        List<TransactionResponse> transactions = transactionService.getAllTransaction()
                .stream()
                .filter(transaction -> {
                    LocalDate transactionDate = transaction.getCreationDate().toLocalDate(); // Asegúrate que getDate() devuelve LocalDateTime
                    return !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

        List<AccountingAdjustmentResponse> adjustments = accountingAdjustmentService.getAllAccountingAdjustments()
                .stream()
                .filter(adjustment -> {
                    LocalDate adjustmentDate = adjustment.getCreationDate().toLocalDate(); // Asegúrate que getCreationDate() devuelve LocalDateTime
                    return !adjustmentDate.isBefore(startDate) && !adjustmentDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

        return new AccountingPeriodDataResponse(transactions, adjustments);
    }
}
