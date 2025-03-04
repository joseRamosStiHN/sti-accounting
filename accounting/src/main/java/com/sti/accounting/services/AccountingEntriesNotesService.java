package com.sti.accounting.services;

import com.sti.accounting.entities.AccountingPeriodEntity;
import com.sti.accounting.models.*;
import org.springframework.stereotype.Service;

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


    //ToDo: Considerar crear la relacion en la entidad de transacciones con ajuste, y las ND y NC
    public AccountingEntriesNotesResponse getAccountingEntriesNotes() {

        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        // Obtener transacciones, ajustes y notas para el periodo especificado
        List<TransactionResponse> transactions = transactionService.getAllTransaction()
                .stream()
                .filter(transaction -> transaction.getAccountingPeriodId().equals(activePeriod.getId()))
                .toList();

        List<AccountingAdjustmentResponse> adjustments = accountingAdjustmentService.getAllAccountingAdjustments()
                .stream()
                .filter(adjustment -> adjustment.getAccountingPeriodId().equals(activePeriod.getId()))
                .toList();

        List<DebitNotesResponse> debitNotes = debitNotesService.getAllDebitNotes()
                .stream()
                .filter(debitNote -> debitNote.getAccountingPeriodId().equals(activePeriod.getId()))
                .toList();

        List<CreditNotesResponse> creditNotes = creditNotesService.getAllCreditNotes()
                .stream()
                .filter(creditNote -> creditNote.getAccountingPeriodId().equals(activePeriod.getId()))
                .toList();

        return new AccountingEntriesNotesResponse(transactions, adjustments, debitNotes, creditNotes);
    }

}
