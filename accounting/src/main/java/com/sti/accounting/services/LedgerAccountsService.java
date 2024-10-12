package com.sti.accounting.services;

import com.sti.accounting.entities.TransactionDetailEntity;
import com.sti.accounting.entities.TransactionEntity;
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

    public LedgerAccountsService(ITransactionRepository transactionRepository, AccountingPeriodService accountingPeriodService,AccountingJournalService accountingJournalService) {
        this.transactionRepository = transactionRepository;
        this.accountingPeriodService = accountingPeriodService;
        this.accountingJournalService = accountingJournalService;
    }


    public List<LedgerAccountsResponse> generateLedgerAccountsReport() {
        LocalDate startDate = accountingPeriodService.getDateStartPeriodAccountingActive();
        LocalDate endDate = accountingPeriodService.getActiveAccountingPeriodEndDate();

        List<TransactionEntity> transactionList = transactionRepository.findByCreateAtDateBetween(startDate, endDate);

        Map<Long, List<TransactionEntity>> diaryTransactionsMap = new HashMap<>();

        for (TransactionEntity transaction : transactionList) {
            Long diaryId = transaction.getAccountingJournal().getId();
            diaryTransactionsMap.computeIfAbsent(diaryId, id -> new ArrayList<>()).add(transaction);
        }

        List<LedgerAccountsResponse> ledgerAccountsResponses = new ArrayList<>();
        for (Map.Entry<Long, List<TransactionEntity>> entry : diaryTransactionsMap.entrySet()) {
            Long diaryId = entry.getKey();
            List<TransactionEntity> transactions = entry.getValue();

            LedgerAccountsResponse ledgerAccountsResponse = new LedgerAccountsResponse();
            ledgerAccountsResponse.setDiaryId(diaryId);
            ledgerAccountsResponse.setDiaryName(accountingJournalService.getDiaryName(diaryId));

            List<LedgerAccountsResponse.TransactionResponse> transactionResponses = new ArrayList<>();
            for (TransactionEntity transaction : transactions) {
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
            }
            ledgerAccountsResponse.setTransactions(transactionResponses);
            ledgerAccountsResponses.add(ledgerAccountsResponse);
        }

        return ledgerAccountsResponses;
    }
}
