package com.sti.accounting.services;

import com.sti.accounting.entities.AccountingPeriodEntity;
import com.sti.accounting.models.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class AccountingEntriesNotesService {

    private final TransactionService transactionService;
    private final AccountingAdjustmentService accountingAdjustmentService;
    private final AccountingPeriodService accountingPeriodService;
    private final CreditNotesService creditNotesService;
    private final DebitNotesService debitNotesService;

    public AccountingEntriesNotesService(TransactionService transactionService, AccountingAdjustmentService accountingAdjustmentService, AccountingPeriodService accountingPeriodService, CreditNotesService creditNotesService, DebitNotesService debitNotesService) {
        this.transactionService = transactionService;
        this.accountingAdjustmentService = accountingAdjustmentService;
        this.accountingPeriodService = accountingPeriodService;
        this.creditNotesService = creditNotesService;
        this.debitNotesService = debitNotesService;
    }


    public AccountingEntriesNotesResponse getAccountingEntriesNotes() {
        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        LocalDate startDate = activePeriod.getStartPeriod().toLocalDate();
        LocalDate endDate = activePeriod.getEndPeriod().toLocalDate();

        // Obtener transacciones, ajustes y notas para el periodo activo
        List<TransactionResponse> transactions = transactionService.getAllTransaction()
                .stream()
                .filter(transaction -> {
                    LocalDate transactionDate = transaction.getDate();
                    return !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate);
                })
                .toList();

        List<AccountingAdjustmentResponse> adjustments = accountingAdjustmentService.getAllAccountingAdjustments()
                .stream()
                .filter(adjustment -> {
                    LocalDate adjustmentDate = adjustment.getCreationDate().toLocalDate();
                    return !adjustmentDate.isBefore(startDate) && !adjustmentDate.isAfter(endDate);
                })
                .toList();

        List<DebitNotesResponse> debitNotes = debitNotesService.getAllDebitNotes()
                .stream()
                .filter(debitNote -> {
                    LocalDate debitNoteDate = debitNote.getCreationDate().toLocalDate();
                    return !debitNoteDate.isBefore(startDate) && !debitNoteDate.isAfter(endDate);
                })
                .toList();

        List<CreditNotesResponse> creditNotes = creditNotesService.getAllCreditNotes()
                .stream()
                .filter(creditNote -> {
                    LocalDate creditNoteDate = creditNote.getCreationDate().toLocalDate();
                    return !creditNoteDate.isBefore(startDate) && !creditNoteDate.isAfter(endDate);
                })
                .toList();

        return new AccountingEntriesNotesResponse(transactions, adjustments, debitNotes, creditNotes);
    }


}
