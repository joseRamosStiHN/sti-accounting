package com.sti.accounting.services;

import com.sti.accounting.entities.AccountEntity;
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

    public LedgerAccountsService(ITransactionRepository transactionRepository, AccountingPeriodService accountingPeriodService) {
        this.transactionRepository = transactionRepository;
        this.accountingPeriodService = accountingPeriodService;

    }


    public List<LedgerAccountsResponse> generateLedgerAccountsReport() {
        LocalDate startDate = accountingPeriodService.getDateStartPeriodAccountingActive();
        LocalDate endDate = accountingPeriodService.getActiveAccountingPeriodEndDate();

        List<TransactionEntity> transactions = transactionRepository.findByCreateAtDateBetween(startDate, endDate);

        Map<Long, LedgerAccountsResponse> parentAccountMap = new HashMap<>();

        for (TransactionEntity transaction : transactions) {
            for (TransactionDetailEntity detail : transaction.getTransactionDetail()) {
                AccountEntity account = detail.getAccount();
                Long parentId = account.getParent() != null ? account.getParent().getId() : null;

                if (parentId == null) continue;

                LedgerAccountsResponse parentResponse = parentAccountMap.computeIfAbsent(parentId, id -> {
                    LedgerAccountsResponse response = new LedgerAccountsResponse();
                    response.setParentAccountId(id);
                    response.setParentAccountName(account.getParent().getDescription());
                    response.setParentAccountCode(account.getParent().getCode());
                    response.setChildAccounts(new ArrayList<>());
                    return response;
                });

                LedgerAccountsResponse.ChildAccountResponse childResponse = parentResponse.getChildAccounts().stream()
                        .filter(child -> child.getAccountId().equals(account.getId()))
                        .findFirst()
                        .orElseGet(() -> {
                            LedgerAccountsResponse.ChildAccountResponse newChild = new LedgerAccountsResponse.ChildAccountResponse();
                            newChild.setAccountId(account.getId());
                            newChild.setAccountName(account.getDescription());
                            newChild.setAccountCode(account.getCode());
                            newChild.setTransactions(new ArrayList<>());
                            parentResponse.getChildAccounts().add(newChild);
                            return newChild;
                        });

                LedgerAccountsResponse.TransactionDetailResponse detailResponse = new LedgerAccountsResponse.TransactionDetailResponse();
                detailResponse.setId(detail.getId());
                detailResponse.setDescription(detail.getTransaction().getDescriptionPda());
                detailResponse.setAmount(detail.getAmount());
                detailResponse.setEntryType(detail.getMotion() == Motion.D ? "Debito" : "Credito");
                detailResponse.setShortEntryType(detail.getMotion() == Motion.D ? "D" : "C");
                detailResponse.setAccountName(account.getDescription());
                detailResponse.setAccountCode(account.getCode());
                detailResponse.setDate(detail.getTransaction().getCreateAtDate());
                detailResponse.setCreationDate(detail.getTransaction().getCreateAtTime());
                childResponse.getTransactions().add(detailResponse);
            }
        }

        return new ArrayList<>(parentAccountMap.values());
    }

}
