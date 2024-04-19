package com.sti.accounting.services;

import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.entities.TransactionDetailEntity;
import com.sti.accounting.entities.TransactionEntity;
import com.sti.accounting.models.TransactionDetailRequest;
import com.sti.accounting.models.TransactionRequest;
import com.sti.accounting.repositories.IAccountRepository;
import com.sti.accounting.repositories.ITransactionRepository;
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

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    private final ITransactionRepository transactionRepository;
    private final IAccountRepository iAccountRepository;

    public TransactionService(ITransactionRepository transactionRepository, IAccountRepository iAccountRepository) {
        this.transactionRepository = transactionRepository;
        this.iAccountRepository = iAccountRepository;

    }

    @Transactional
    public TransactionEntity createTransaction(TransactionRequest transactionRequest) {
        logger.info("creating transaction");
        try {

            TransactionEntity transactionEntity = new TransactionEntity();

            transactionEntity.setCreateAtDate(transactionRequest.getCreateAtDate());
            transactionEntity.setStatus(0L);
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
                        .orElseThrow(() -> new BadRequestException("Account with id " + detailRequest.getAccountId() + " not found"));
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
            logger.error("Error creating transaction: {}", e.getMessage());
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
                                        "Account with ID " + detailRequest.getAccountId() + " not found"));

                        existingDetail.setAccount(account);
                        existingDetail.setAmount(detailRequest.getAmount());
                        existingDetail.setMotion(detailRequest.getMotion());
                        updatedDetails.add(existingDetail);
                    } else {
                        AccountEntity account = iAccountRepository.findById(detailRequest.getAccountId())
                                .orElseThrow(() -> new BadRequestException(
                                        "Account with ID " + detailRequest.getAccountId() + " not found"));

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
            logger.error("Error updating transaction with ID {}: {}", id, e.getMessage());
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

            existingTransaction.setStatus(1L);
            transactionRepository.save(existingTransaction);

        } catch (Exception e) {
            logger.error("Error changing status of transaction with id {}: {}", transactionId, e.getMessage());
            throw new RuntimeException("Error changing status of transaction: " + e.getMessage());
        }
    }
}
