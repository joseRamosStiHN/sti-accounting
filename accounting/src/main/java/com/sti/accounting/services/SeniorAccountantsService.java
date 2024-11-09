package com.sti.accounting.services;

import com.sti.accounting.entities.AccountingPeriodEntity;
import com.sti.accounting.models.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SeniorAccountantsService {

    private final TransactionService transactionService;


    public SeniorAccountantsService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }


    public List<SeniorAccountsResponse> getSeniorAccountants() {
        Map<String, List<AccountTransactionDTO>> transactionAccounts = transactionService.getTransactionAccountsByActivePeriod();

        List<SeniorAccountsResponse> seniorAccountsResponsesList = new ArrayList<>();

        for (Map.Entry<String, List<AccountTransactionDTO>> transaction : transactionAccounts.entrySet()) {
            SeniorAccountsResponse seniorAccountsResponse = new SeniorAccountsResponse();
            seniorAccountsResponse.setName(transaction.getValue().getFirst().getDescription());
            seniorAccountsResponse.setCode(transaction.getValue().getFirst().getCode());
            seniorAccountsResponse.setFatherAccount(transaction.getValue().getFirst().getFatherAccount());
            seniorAccountsResponse.setTypeAccount(transaction.getValue().getFirst().getCategoryName());

            BigDecimal totalDebit = transaction.getValue().stream()
                    .filter(dto -> "D".equalsIgnoreCase(dto.getMotion()))
                    .map(dto -> new BigDecimal(dto.getAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCredit = transaction.getValue().stream()
                    .filter(dto -> "C".equalsIgnoreCase(dto.getMotion()))
                    .map(dto -> new BigDecimal(dto.getAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            seniorAccountsResponse.setTotalDebit(totalDebit);
            seniorAccountsResponse.setTotalCredit(totalCredit);

            if (totalDebit.compareTo(totalCredit) >= 0) {
                seniorAccountsResponse.setBalance(totalDebit.subtract(totalCredit));
            } else {
                seniorAccountsResponse.setBalance(totalCredit.subtract(totalDebit));
            }

            List<SeniorAccountsTransactionResponse> seniorAccountsList = new ArrayList<>();
            for (AccountTransactionDTO transactionDTO : transaction.getValue()) {
                SeniorAccountsTransactionResponse seniorAccountsTransactionResponse = new SeniorAccountsTransactionResponse();
                seniorAccountsTransactionResponse.setName(transactionDTO.getDescription());
                if (transactionDTO.getMotion().equalsIgnoreCase("D")) {
                    seniorAccountsTransactionResponse.setDebitAmount(new BigDecimal(transactionDTO.getAmount()));
                    seniorAccountsTransactionResponse.setCreditAmount(BigDecimal.ZERO);
                } else if (transactionDTO.getMotion().equalsIgnoreCase("C")) {
                    seniorAccountsTransactionResponse.setDebitAmount(BigDecimal.ZERO);
                    seniorAccountsTransactionResponse.setCreditAmount(new BigDecimal(transactionDTO.getAmount()));
                }
                if (seniorAccountsTransactionResponse.getDebitAmount().compareTo(seniorAccountsTransactionResponse.getCreditAmount()) >= 0) {
                    seniorAccountsTransactionResponse.setBalance(seniorAccountsTransactionResponse.getDebitAmount().subtract(seniorAccountsTransactionResponse.getCreditAmount()));
                } else {
                    seniorAccountsTransactionResponse.setBalance(seniorAccountsTransactionResponse.getCreditAmount().subtract(seniorAccountsTransactionResponse.getDebitAmount()));
                }
                seniorAccountsTransactionResponse.setMotion(transactionDTO.getTypeMovement());
                seniorAccountsTransactionResponse.setNumberPda(Long.parseLong(transactionDTO.getNumberPda()));
                seniorAccountsTransactionResponse.setDate(transactionDTO.getDate());
                seniorAccountsList.add(seniorAccountsTransactionResponse);

            }
            seniorAccountsResponse.setTransaction(seniorAccountsList);
            seniorAccountsResponsesList.add(seniorAccountsResponse);
        }
        return seniorAccountsResponsesList;
    }
}
