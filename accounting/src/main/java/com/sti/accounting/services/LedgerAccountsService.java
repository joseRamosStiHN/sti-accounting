package com.sti.accounting.services;

import com.sti.accounting.entities.TransactionDetailEntity;
import com.sti.accounting.entities.TransactionEntity;
import com.sti.accounting.models.AccountingAdjustmentResponse;
import com.sti.accounting.models.CreditNotesResponse;
import com.sti.accounting.models.DebitNotesResponse;
import com.sti.accounting.models.LedgerAccountsResponse;
import com.sti.accounting.repositories.ITransactionRepository;
import com.sti.accounting.utils.Motion;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LedgerAccountsService {

    private final ITransactionRepository transactionRepository;
    private final AccountingPeriodService accountingPeriodService;
    private final AccountingJournalService accountingJournalService;
    private final AccountingAdjustmentService accountingAdjustmentService;
    private final CreditNotesService creditNotesService;
    private final DebitNotesService debitNotesService;

    public LedgerAccountsService(ITransactionRepository transactionRepository,
                                 AccountingPeriodService accountingPeriodService,
                                 AccountingJournalService accountingJournalService,
                                 AccountingAdjustmentService accountingAdjustmentService,
                                 CreditNotesService creditNotesService,
                                 DebitNotesService debitNotesService) {
        this.transactionRepository = transactionRepository;
        this.accountingPeriodService = accountingPeriodService;
        this.accountingJournalService = accountingJournalService;
        this.accountingAdjustmentService = accountingAdjustmentService;
        this.creditNotesService = creditNotesService;
        this.debitNotesService = debitNotesService;
    }

    public List<LedgerAccountsResponse> generateLedgerAccountsReport() {
        LocalDate startDate = accountingPeriodService.getDateStartPeriodAccountingActive();
        LocalDate endDate = accountingPeriodService.getActiveAccountingPeriodEndDate();

        // Obtener transacciones, ajustes y notas dentro del mismo período
        List<TransactionEntity> transactionList = transactionRepository.findByCreateAtDateBetween(startDate, endDate);

        List<AccountingAdjustmentResponse> adjustments = accountingAdjustmentService.getAllAccountingAdjustments()
                .stream()
                .filter(adjustment -> {
                    LocalDate adjustmentDate = adjustment.getCreationDate().toLocalDate();
                    return !adjustmentDate.isBefore(startDate) && !adjustmentDate.isAfter(endDate);
                })
                .toList();

        List<CreditNotesResponse> creditNotes = creditNotesService.getAllCreditNotes()
                .stream()
                .filter(creditNote -> {
                    LocalDate creditNoteDate = creditNote.getCreationDate().toLocalDate();
                    return !creditNoteDate.isBefore(startDate) && !creditNoteDate.isAfter(endDate);
                })
                .toList();

        List<DebitNotesResponse> debitNotes = debitNotesService.getAllDebitNotes()
                .stream()
                .filter(debitNote -> {
                    LocalDate debitNoteDate = debitNote.getCreationDate().toLocalDate();
                    return !debitNoteDate.isBefore(startDate) && !debitNoteDate.isAfter(endDate);
                })
                .toList();

        // Agrupar por diario (ID)
        Map<Long, List<Object>> diaryEntriesMap = new HashMap<>();

        addTransactionsToMap(diaryEntriesMap, transactionList);
        addAdjustmentsToMap(diaryEntriesMap, adjustments);
        addCreditNotesToMap(diaryEntriesMap, creditNotes);
        addDebitNotesToMap(diaryEntriesMap, debitNotes);

        // Construir la respuesta final agrupada por diario
        return buildLedgerAccountsResponse(diaryEntriesMap);
    }

    private void addTransactionsToMap(Map<Long, List<Object>> diaryEntriesMap, List<TransactionEntity> transactions) {
        for (TransactionEntity transaction : transactions) {
            Long diaryId = transaction.getAccountingJournal().getId();
            diaryEntriesMap.computeIfAbsent(diaryId, id -> new ArrayList<>()).add(transaction);
        }
    }

    private void addAdjustmentsToMap(Map<Long, List<Object>> diaryEntriesMap, List<AccountingAdjustmentResponse> adjustments) {
        for (AccountingAdjustmentResponse adjustment : adjustments) {
            Long diaryId = adjustment.getDiaryType();
            diaryEntriesMap.computeIfAbsent(diaryId, id -> new ArrayList<>()).add(adjustment);
        }
    }

    private void addCreditNotesToMap(Map<Long, List<Object>> diaryEntriesMap, List<CreditNotesResponse> creditNotes) {
        for (CreditNotesResponse creditNote : creditNotes) {
            Long diaryId = creditNote.getDiaryType();
            diaryEntriesMap.computeIfAbsent(diaryId, id -> new ArrayList<>()).add(creditNote);
        }
    }

    private void addDebitNotesToMap(Map<Long, List<Object>> diaryEntriesMap, List<DebitNotesResponse> debitNotes) {
        for (DebitNotesResponse debitNote : debitNotes) {
            Long diaryId = debitNote.getDiaryType();
            diaryEntriesMap.computeIfAbsent(diaryId, id -> new ArrayList<>()).add(debitNote);
        }
    }

    private List<LedgerAccountsResponse> buildLedgerAccountsResponse(Map<Long, List<Object>> diaryEntriesMap) {
        List<LedgerAccountsResponse> ledgerAccountsResponses = new ArrayList<>();
        for (Map.Entry<Long, List<Object>> entry : diaryEntriesMap.entrySet()) {
            Long diaryId = entry.getKey();
            List<Object> entries = entry.getValue();

            LedgerAccountsResponse ledgerAccountsResponse = new LedgerAccountsResponse();
            ledgerAccountsResponse.setDiaryId(diaryId);
            ledgerAccountsResponse.setDiaryName(accountingJournalService.getDiaryName(diaryId));

            // Listas para los diferentes tipos de respuestas
            List<LedgerAccountsResponse.TransactionResponse> transactionResponses = new ArrayList<>();
            List<AccountingAdjustmentResponse> adjustmentResponses = new ArrayList<>();
            List<CreditNotesResponse> creditNotesResponses = new ArrayList<>();
            List<DebitNotesResponse> debitNotesResponses = new ArrayList<>();

            for (Object entryObject : entries) {
                if (entryObject instanceof TransactionEntity transaction) {
                    LedgerAccountsResponse.TransactionResponse transactionResponse = new LedgerAccountsResponse.TransactionResponse();
                    transactionResponse.setId(transaction.getId());
                    transactionResponse.setDescription(transaction.getDescriptionPda());
                    transactionResponse.setReference(transaction.getReference());
                    transactionResponse.setCreationDate(transaction.getCreateAtTime());
                    transactionResponse.setDate(transaction.getCreateAtDate());

                    List<LedgerAccountsResponse.TransactionDetailResponse> transactionDetailResponses = new ArrayList<>();
                    for (TransactionDetailEntity detail : transaction.getTransactionDetail()) {
                        LedgerAccountsResponse.TransactionDetailResponse detailResponse = new LedgerAccountsResponse.TransactionDetailResponse();
                        detailResponse.setId(detail.getId());
                        detailResponse.setEntryType(detail.getMotion() == Motion.D ? "Debito" : "Credito");
                        detailResponse.setShortEntryType(detail.getMotion() == Motion.D ? "D" : "C");
                        detailResponse.setAccountCode(detail.getAccount().getCode());
                        detailResponse.setAccountName(detail.getAccount().getDescription());
                        detailResponse.setAmount(detail.getAmount());
                        transactionDetailResponses.add(detailResponse);
                    }
                    transactionResponse.setTransactionsDetail(transactionDetailResponses);
                    transactionResponses.add(transactionResponse);
                } else if (entryObject instanceof AccountingAdjustmentResponse adjustment) {
                    adjustmentResponses.add(adjustment);
                } else if (entryObject instanceof CreditNotesResponse creditNote) {
                    creditNotesResponses.add(creditNote);
                } else if (entryObject instanceof DebitNotesResponse debitNote) {
                    debitNotesResponses.add(debitNote);
                }
            }

            // Asignar las listas correspondientes
            ledgerAccountsResponse.setTransactions(transactionResponses);
            ledgerAccountsResponse.setAdjustments(adjustmentResponses);
            ledgerAccountsResponse.setCreditNotes(creditNotesResponses);
            ledgerAccountsResponse.setDebitNotes(debitNotesResponses);

            ledgerAccountsResponses.add(ledgerAccountsResponse);
        }

        return ledgerAccountsResponses;
    }
}
