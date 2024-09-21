package com.sti.accounting.services;

import com.sti.accounting.entities.*;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.IAccountRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeneralBalanceService {

    private static final Logger logger = LoggerFactory.getLogger(GeneralBalanceService.class);

    private final IAccountRepository iAccountRepository;
    private final JournalEntryService journalEntryService;

    public GeneralBalanceService(IAccountRepository iAccountRepository, JournalEntryService journalEntryService) {
        this.iAccountRepository = iAccountRepository;
        this.journalEntryService = journalEntryService;
    }

    @Transactional
    public BalanceGeneralResponse getBalanceGeneral() {
        logger.info("Generating balance general");

        List<AccountEntity> accounts = iAccountRepository.findAll();
        AccountingPeriodDataResponse data = journalEntryService.getTransactionAdjustment();

        Map<Long, BigDecimal> debitSumMap = calculateDebitSums(accounts, data);
        Map<Long, BigDecimal> creditSumMap = calculateCreditSums(accounts, data);

        List<GeneralBalanceResponse> assets = new ArrayList<>();
        List<GeneralBalanceResponse> liabilities = new ArrayList<>();
        List<GeneralBalanceResponse> equity = new ArrayList<>();

        for (AccountEntity account : accounts) {
            GeneralBalanceResponse item = createGeneralBalanceResponse(account, debitSumMap, creditSumMap);
            if (account.getCode().startsWith("1")) {
                assets.add(item);
            } else if (account.getCode().startsWith("2")) {
                liabilities.add(item);
            } else if (account.getCode().startsWith("3")) {
                equity.add(item);
            }
        }

        BalanceGeneralResponse response = new BalanceGeneralResponse();
        response.setAssets(assets);
        response.setLiabilities(liabilities);
        response.setEquity(equity);
        return response;
    }

    private Map<Long, BigDecimal> calculateDebitSums(List<AccountEntity> accounts, AccountingPeriodDataResponse data) {
        Map<Long, BigDecimal> debitSumMap = new HashMap<>();
        for (AccountEntity account : accounts) {
            debitSumMap.put(account.getId(), BigDecimal.ZERO);
        }

        for (TransactionResponse transaction : data.getTransactions()) {
            for (TransactionDetailResponse detail : transaction.getTransactionDetails()) {
                if (detail.getShortEntryType().equals("D")) {
                    debitSumMap.put(detail.getAccountId(), debitSumMap.get(detail.getAccountId()).add(detail.getAmount()));
                }
            }
        }

        for (AccountingAdjustmentResponse adjustment : data.getAdjustments()) {
            for (AdjustmentDetailResponse detail : adjustment.getAdjustmentDetails()) {
                if (detail.getShortEntryType().equals("D")) {
                    debitSumMap.put(detail.getAccountId(), debitSumMap.get(detail.getAccountId()).add(detail.getAmount()));
                }
            }
        }

        return debitSumMap;
    }

    private Map<Long, BigDecimal> calculateCreditSums(List<AccountEntity> accounts, AccountingPeriodDataResponse data) {
        Map<Long, BigDecimal> creditSumMap = new HashMap<>();
        for (AccountEntity account : accounts) {
            creditSumMap.put(account.getId(), BigDecimal.ZERO);
        }

        for (TransactionResponse transaction : data.getTransactions()) {
            for (TransactionDetailResponse detail : transaction.getTransactionDetails()) {
                if (detail.getShortEntryType().equals("C")) {
                    creditSumMap.put(detail.getAccountId(), creditSumMap.get(detail.getAccountId()).add(detail.getAmount()));
                }
            }
        }

        for (AccountingAdjustmentResponse adjustment : data.getAdjustments()) {
            for (AdjustmentDetailResponse detail : adjustment.getAdjustmentDetails()) {
                if (detail.getShortEntryType().equals("C")) {
                    creditSumMap.put(detail.getAccountId(), creditSumMap.get(detail.getAccountId()).add(detail.getAmount()));
                }
            }
        }

        return creditSumMap;
    }

    private GeneralBalanceResponse createGeneralBalanceResponse(AccountEntity account, Map<Long, BigDecimal> debitSumMap, Map<Long, BigDecimal> creditSumMap) {
        GeneralBalanceResponse item = new GeneralBalanceResponse();
        item.setAccountId(account.getId());
        item.setAccountName(account.getDescription());
        BigDecimal debit = debitSumMap.get(account.getId());
        BigDecimal credit = creditSumMap.get(account.getId());
        item.setDebit(debit);
        item.setCredit(credit);
        item.setBalance(debit.subtract(credit).abs());
        return item;
    }
}