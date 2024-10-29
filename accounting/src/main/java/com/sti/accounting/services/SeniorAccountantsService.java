package com.sti.accounting.services;

import com.sti.accounting.entities.AccountingPeriodEntity;
import com.sti.accounting.models.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class SeniorAccountantsService {

    private final AccountingAdjustmentService accountingAdjustmentService;
    private final TransactionService transactionService;
    private final AccountingPeriodService accountingPeriodService;
    private final CreditNotesService creditNotesService;
    private final DebitNotesService debitNotesService;

    public SeniorAccountantsService(AccountingAdjustmentService accountingAdjustmentService, TransactionService transactionService, AccountingPeriodService accountingPeriodService, CreditNotesService creditNotesService, DebitNotesService debitNotesService) {
        this.accountingAdjustmentService = accountingAdjustmentService;
        this.transactionService = transactionService;
        this.accountingPeriodService = accountingPeriodService;
        this.creditNotesService = creditNotesService;
        this.debitNotesService = debitNotesService;
    }


    public SeniorAccountantsResponse getSeniorAccountants() {
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

        return new SeniorAccountantsResponse(transactions, adjustments, debitNotes, creditNotes);
    }


    public List<SeniorAccountsResponse> getAccountsWhitTransactions() {
        Map<String, List<AccountTransactionDTO>> transactionAccounts  =   transactionService.getTransactionAccounts();
        List<SeniorAccountsResponse> seniorAccountsResponsesList = new ArrayList<>();

        for (Map.Entry<String, List<AccountTransactionDTO>> transaction : transactionAccounts.entrySet()) {
            SeniorAccountsResponse seniorAccountsResponse = new SeniorAccountsResponse();
            seniorAccountsResponse.setName(transaction.getValue().getFirst().getDescription());
            seniorAccountsResponse.setCode(transaction.getValue().getFirst().getCode());
            seniorAccountsResponse.setCuentaPadre(transaction.getValue().getFirst().getCuentaPadre());
            seniorAccountsResponse.setTipoCuenta(transaction.getValue().getFirst().getCategoryName());

            BigDecimal totalDebe = transaction.getValue().stream()
                    .filter(dto -> "D".equalsIgnoreCase(dto.getMotion()))
                    .map(dto -> new BigDecimal(dto.getAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalHaber = transaction.getValue().stream()
                    .filter(dto -> "C".equalsIgnoreCase(dto.getMotion()))
                    .map(dto -> new BigDecimal(dto.getAmount()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            seniorAccountsResponse.setTotalDebe(totalDebe);
            seniorAccountsResponse.setTotalHaber(totalHaber);

            if (totalDebe.compareTo(totalHaber) >= 0) {
                seniorAccountsResponse.setBalance(totalDebe.subtract(totalHaber));
            } else  {
                seniorAccountsResponse.setBalance(totalHaber.subtract(totalDebe));
            }

            List<SeniorAccountsTransactionResponse> seniorAccountsList = new ArrayList<>();
            for (AccountTransactionDTO transactionDTO : transaction.getValue()){
                SeniorAccountsTransactionResponse seniorAccountsTransactionResponse = new SeniorAccountsTransactionResponse();
                seniorAccountsTransactionResponse.setName(transactionDTO.getDescription());
                if (transactionDTO.getMotion().equalsIgnoreCase("D")){
                    seniorAccountsTransactionResponse.setDebe( new BigDecimal(transactionDTO.getAmount()));
                    seniorAccountsTransactionResponse.setHaber(BigDecimal.ZERO);
                }else  if(transactionDTO.getMotion().equalsIgnoreCase("C")){
                    seniorAccountsTransactionResponse.setDebe(BigDecimal.ZERO);
                    seniorAccountsTransactionResponse.setHaber( new BigDecimal(transactionDTO.getAmount()));
                }
                if (seniorAccountsTransactionResponse.getDebe().compareTo(seniorAccountsTransactionResponse.getHaber()) >= 0) {
                    seniorAccountsTransactionResponse.setBalance(seniorAccountsTransactionResponse.getDebe().subtract(seniorAccountsTransactionResponse.getHaber()));
                } else  {
                    seniorAccountsTransactionResponse.setBalance(seniorAccountsTransactionResponse.getHaber().subtract(seniorAccountsTransactionResponse.getDebe()));
                }
                seniorAccountsTransactionResponse.setMovimiento(transactionDTO.getMovimiento());
                seniorAccountsTransactionResponse.setNumberPda(Long.parseLong(transactionDTO.getNumberPda()));
                seniorAccountsList.add(seniorAccountsTransactionResponse);

            }
            seniorAccountsResponse.setTransaction(seniorAccountsList);
            seniorAccountsResponsesList.add(seniorAccountsResponse);
        }
        return seniorAccountsResponsesList;
    }
}
