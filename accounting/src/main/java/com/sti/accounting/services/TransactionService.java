package com.sti.accounting.services;

import com.sti.accounting.entities.*;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.IAccountRepository;
import com.sti.accounting.repositories.ITransactionRepository;
import com.sti.accounting.repositories.ITransactionSumViewRepository;
import com.sti.accounting.repositories.ITransactionBalanceGeneralRepository;
import com.sti.accounting.utils.AppUtil;
import com.sti.accounting.utils.Motion;
import jakarta.ws.rs.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final ITransactionRepository transactionRepository;
    private final IAccountRepository iAccountRepository;

    private final ITransactionBalanceGeneralRepository view;
    private final ITransactionSumViewRepository viewSum;


    public TransactionService(ITransactionRepository transactionRepository, IAccountRepository iAccountRepository, ITransactionBalanceGeneralRepository view, ITransactionSumViewRepository viewSum) {
        this.transactionRepository = transactionRepository;
        this.iAccountRepository = iAccountRepository;
        this.view = view;
        this.viewSum = viewSum;
    }

    @Transactional
    public TransactionEntity createTransaction(TransactionRequest transactionRequest) {
        logger.info("creating transaction");
        try {

            TransactionEntity transactionEntity = new TransactionEntity();

            transactionEntity.setCreateAtDate(transactionRequest.getCreateAtDate());
            transactionEntity.setStatus(StatusTransaction.DRAFT);
            transactionEntity.setReference(transactionRequest.getReference());
            transactionEntity.setDocumentType(transactionRequest.getDocumentType());
            transactionEntity.setExchangeRate(transactionRequest.getExchangeRate());
            transactionEntity.setDescriptionPda(transactionRequest.getDescriptionPda());
            transactionEntity.setCurrency(transactionRequest.getCurrency());


            BigDecimal totalCredits = transactionRequest.getDetail().stream()
                    .filter(detail -> Motion.C.equals(detail.getMotion()))
                    .map(TransactionDetailRequest::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalDebits = transactionRequest.getDetail().stream()
                    .filter(detail -> Motion.D.equals(detail.getMotion()))
                    .map(TransactionDetailRequest::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);


            if (!totalCredits.equals(totalDebits)) {
                throw new BadRequestException("The values entered in the detail are not balanced");
            }

            List<TransactionDetailEntity> detail = transactionRequest.getDetail().stream().map(detailRequest -> {
                AccountEntity account = iAccountRepository.findById(detailRequest.getAccountId())
                        .orElseThrow(() -> new BadRequestException("Account with id " + detailRequest.getAccountId() + Constant.NOT_FOUND));
                TransactionDetailEntity dto = new TransactionDetailEntity();
                dto.setTransaction(transactionEntity);
                dto.setAccount(account);
                dto.setAmount(detailRequest.getAmount());
                dto.setMotion(detailRequest.getMotion());
                return dto;
            }).collect(Collectors.toList());


            transactionEntity.setTransactionDetail(detail);

            return transactionRepository.save(transactionEntity);
        } catch (Exception e) {
            throw new RuntimeException("Error transaction account: " + e.getMessage());
        }
    }


    @Transactional
    public TransactionEntity updateTransaction(Long id, TransactionRequest transactionRequest) {
        logger.info("Updating transaction with ID: {}", id);
        try {
            TransactionEntity existingTransaction = transactionRepository.findById(id)
                    .orElseThrow(() -> new BadRequestException(
                            String.format("No transaction found with ID: %d", id)));

            existingTransaction.setCreateAtDate(transactionRequest.getCreateAtDate());
            existingTransaction.setReference(transactionRequest.getReference());
            existingTransaction.setDocumentType(transactionRequest.getDocumentType());
            existingTransaction.setExchangeRate(transactionRequest.getExchangeRate());
            existingTransaction.setDescriptionPda(transactionRequest.getDescriptionPda());
            existingTransaction.setNumberPda(transactionRequest.getNumberPda());
            existingTransaction.setCurrency(transactionRequest.getCurrency());

            List<TransactionDetailEntity> updatedDetails = new ArrayList<>();
            if (transactionRequest.getDetail() != null) {
                for (TransactionDetailRequest detailRequest : transactionRequest.getDetail()) {
                    if (detailRequest.getId() != null) {
                        TransactionDetailEntity existingDetail = existingTransaction.getTransactionDetail().stream()
                                .filter(detail -> detail.getId().equals(detailRequest.getId()))
                                .findFirst()
                                .orElseThrow(() -> new BadRequestException(
                                        String.format("No detail entity found with ID: %d", detailRequest.getId())));

                        AccountEntity account = iAccountRepository.findById(detailRequest.getAccountId())
                                .orElseThrow(() -> new BadRequestException(
                                        "Account with ID " + detailRequest.getAccountId() + Constant.NOT_FOUND));

                        existingDetail.setAccount(account);
                        existingDetail.setAmount(detailRequest.getAmount());
                        existingDetail.setMotion(detailRequest.getMotion());
                        updatedDetails.add(existingDetail);
                    } else {
                        AccountEntity account = iAccountRepository.findById(detailRequest.getAccountId())
                                .orElseThrow(() -> new BadRequestException(
                                        "Account with ID " + detailRequest.getAccountId() + Constant.NOT_FOUND));

                        TransactionDetailEntity newDetail = new TransactionDetailEntity();
                        newDetail.setTransaction(existingTransaction);
                        newDetail.setAccount(account);
                        newDetail.setAmount(detailRequest.getAmount());
                        newDetail.setMotion(detailRequest.getMotion());
                        updatedDetails.add(newDetail);
                    }
                }
            }

            existingTransaction.getTransactionDetail().clear();
            existingTransaction.getTransactionDetail().addAll(updatedDetails);

            return transactionRepository.save(existingTransaction);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error updating transaction: " + e.getMessage());
        }
    }

    @Transactional
    public void changeTransactionStatus(Long transactionId) {
        logger.info("Changing status of transaction with id {}", transactionId);
        try {
            TransactionEntity existingTransaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new BadRequestException(
                            String.format("No transaction found with ID: %d", transactionId)));

            existingTransaction.setStatus(StatusTransaction.SUCCESS);
            transactionRepository.save(existingTransaction);

        } catch (Exception e) {
            throw new RuntimeException("Error changing status of transaction: " + e.getMessage());
        }
    }


    @Transactional
    public List<BalanceGeneralResponse> getBalanceGeneral() {
        List<TransactionBalanceGeneralEntity> response = new ArrayList<>();
        Iterable<TransactionBalanceGeneralEntity> data = view.findAll();
        data.forEach(response::add);
        return AppUtil.buildBalanceGeneral(response);
    }

    @Transactional
    public List<TransactionSumViewEntity> getTrxSum(TransactionByPeriodRequest transactionRequest) {
        return  viewSum.findTrx(transactionRequest.getAccount(),transactionRequest.getInitDate(),transactionRequest.getEndDate());
    }

}
