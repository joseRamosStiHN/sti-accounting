package com.sti.accounting.services;

import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.entities.TransactionDetailEntity;
import com.sti.accounting.entities.TransactionEntity;
import com.sti.accounting.models.LedgerAccountDetailResponse;
import com.sti.accounting.models.LedgerAccountsResponse;
import com.sti.accounting.repositories.IAccountRepository;
import com.sti.accounting.repositories.ITransactionRepository;
import com.sti.accounting.utils.Motion;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LedgerAccountsService {

    private final ITransactionRepository transactionRepository;
    private final IAccountRepository iAccountRepository;
    private final AccountingPeriodService accountingPeriodService;

    public LedgerAccountsService(ITransactionRepository transactionRepository, IAccountRepository iAccountRepository, AccountingPeriodService accountingPeriodService) {
        this.transactionRepository = transactionRepository;
        this.iAccountRepository = iAccountRepository;
        this.accountingPeriodService = accountingPeriodService;

    }

    public List<LedgerAccountsResponse> getLedgerAccountsDetail() {
        List<LedgerAccountsResponse> ledgerAccounts = new ArrayList<>();

        LocalDate startDate = accountingPeriodService.getDateStartPeriodAccountingActive();
        LocalDate endDate = accountingPeriodService.getActiveAccountingPeriodEndDate();

        List<TransactionEntity> transactions = transactionRepository.findByCreateAtDateBetween(startDate, endDate);

        Map<Long, List<TransactionDetailEntity>> transactionDetailsByAccount = transactions.stream()
                .flatMap(t -> t.getTransactionDetail().stream())
                .collect(Collectors.groupingBy(td -> td.getAccount().getId()));

        for (Map.Entry<Long, List<TransactionDetailEntity>> entry : transactionDetailsByAccount.entrySet()) {
            Long accountId = entry.getKey();
            List<TransactionDetailEntity> accountTransactionDetails = entry.getValue();

            BigDecimal debits = BigDecimal.ZERO;
            BigDecimal credits = BigDecimal.ZERO;

            List<LedgerAccountDetailResponse> ledgerAccountDetails = new ArrayList<>();

            for (TransactionDetailEntity detail : accountTransactionDetails) {
                LedgerAccountDetailResponse ledgerAccountDetail = new LedgerAccountDetailResponse();
                ledgerAccountDetail.setTransactionId(detail.getTransaction().getId());
                ledgerAccountDetail.setNumberPda(detail.getTransaction().getNumberPda());
                ledgerAccountDetail.setAccountId(accountId);
                ledgerAccountDetail.setAccountCode(detail.getAccount().getCode());
                ledgerAccountDetail.setAccountName(detail.getAccount().getDescription());
                ledgerAccountDetail.setAccountType(detail.getAccount().getAccountType().getId());
                ledgerAccountDetail.setAccountTypeName(detail.getAccount().getAccountType().getName());
                ledgerAccountDetail.setDate(detail.getTransaction().getCreateAtDate());
                ledgerAccountDetail.setDebit(detail.getMotion() == Motion.D ? detail.getAmount() : BigDecimal.ZERO);
                ledgerAccountDetail.setCredit(detail.getMotion() == Motion.C ? detail.getAmount() : BigDecimal.ZERO);

                ledgerAccountDetails.add(ledgerAccountDetail);

                if (detail.getMotion() == Motion.D) {
                    debits = debits.add(detail.getAmount());
                } else if (detail.getMotion() == Motion.C) {
                    credits = credits.add(detail.getAmount());
                }
            }

            AccountEntity account = iAccountRepository.findById(accountId).orElseThrow();

            LedgerAccountsResponse ledgerAccount = new LedgerAccountsResponse();
            ledgerAccount.setAccountId(accountId);
            ledgerAccount.setAccountCode(account.getCode());
            ledgerAccount.setAccountName(account.getDescription());
            ledgerAccount.setTotalDebits(debits);
            ledgerAccount.setTotalCredits(credits);
            ledgerAccount.setBalance(debits.compareTo(credits) >= 0 ? debits.subtract(credits) : credits.subtract(debits));
            ledgerAccount.setDetails(ledgerAccountDetails);

            ledgerAccounts.add(ledgerAccount);
        }

        return ledgerAccounts;
    }
}
