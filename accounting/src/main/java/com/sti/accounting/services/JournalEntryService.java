package com.sti.accounting.services;

import com.sti.accounting.entities.AccountingPeriodEntity;
import com.sti.accounting.models.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class JournalEntryService {

    private final AccountingAdjustmentService accountingAdjustmentService;
    private final TransactionService transactionService;
    private final AccountingPeriodService accountingPeriodService;
    private final CreditNotesService creditNotesService;
    private final DebitNotesService debitNotesService;

    public JournalEntryService(AccountingAdjustmentService accountingAdjustmentService, TransactionService transactionService, AccountingPeriodService accountingPeriodService, CreditNotesService creditNotesService, DebitNotesService debitNotesService) {
        this.accountingAdjustmentService = accountingAdjustmentService;
        this.transactionService = transactionService;
        this.accountingPeriodService = accountingPeriodService;
        this.creditNotesService = creditNotesService;
        this.debitNotesService = debitNotesService;
    }


    public AccountingPeriodDataResponse getJournalEntry() {
        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        LocalDate startDate = activePeriod.getStartPeriod().toLocalDate();
        LocalDate endDate = activePeriod.getEndPeriod().toLocalDate();

        // Obtener transacciones y ajustes para el periodo activo
        List<TransactionResponse> transactions = transactionService.getAllTransaction()
                .stream()
                .filter(transaction -> {
                    LocalDate transactionDate = transaction.getCreationDate().toLocalDate();
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

        return new AccountingPeriodDataResponse(transactions, adjustments, debitNotes, creditNotes);
    }
}
