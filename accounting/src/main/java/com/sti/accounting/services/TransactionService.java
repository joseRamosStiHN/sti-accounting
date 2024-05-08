package com.sti.accounting.services;

import com.sti.accounting.entities.*;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.*;
import com.sti.accounting.utils.AppUtil;

import com.sti.accounting.utils.Motion;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final ITransactionRepository transactionRepository;
    private final IAccountRepository iAccountRepository;
    private final IDocumentRepository document;

    private final ITransactionBalanceGeneralRepository view;
    private final ITransactionSumViewRepository viewSum;


    public TransactionService(ITransactionRepository transactionRepository, IAccountRepository iAccountRepository,
                              IDocumentRepository document, ITransactionBalanceGeneralRepository view,
                              ITransactionSumViewRepository viewSum) {
        this.transactionRepository = transactionRepository;
        this.iAccountRepository = iAccountRepository;
        this.document = document;
        this.view = view;
        this.viewSum = viewSum;
    }

    public List<TransactionResponse> GetAllTransaction() {
        return transactionRepository.findAll().stream().map(this::entityToResponse).toList();
    }

    public TransactionResponse GetById(Long id) {
        TransactionEntity entity = transactionRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                String.format("Transaction not with ID %d not found", id)));
        return entityToResponse(entity);
    }

    @Transactional
    public TransactionResponse CreateTransaction(TransactionRequest transactionRequest) {
        logger.info("creating transaction");
        TransactionEntity entity = new TransactionEntity();

        // Get Document
        DocumentEntity documentType = document.findById(transactionRequest.getDocumentType())
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                String.format("Document type %d not valid ", transactionRequest.getDocumentType())
                        )
                );
        entity.setDocument(documentType);
        entity.setStatus(StatusTransaction.DRAFT);
        entity.setCurrency(transactionRequest.getCurrency());
        entity.setExchangeRate(transactionRequest.getExchangeRate());
        entity.setReference(transactionRequest.getReference());
        entity.setDescriptionPda(transactionRequest.getDescriptionPda());
        entity.setCreateAtDate(transactionRequest.getCreateAtDate());

        //transaction detail validations
        validateTransactionDetail(transactionRequest.getDetail());

        List<TransactionDetailEntity> transactionDetailEntities = detailToEntity(entity, transactionRequest.getDetail());
        entity.setTransactionDetail(transactionDetailEntities);

        transactionRepository.save(entity);

        return entityToResponse(entity);

//        try {
//
//            TransactionEntity transactionEntity = new TransactionEntity();
//
//            transactionEntity.setCreateAtDate(transactionRequest.getCreateAtDate());
//            transactionEntity.setStatus(StatusTransaction.DRAFT);
//            transactionEntity.setReference(transactionRequest.getReference());
//            transactionEntity.setDocumentType(transactionRequest.getDocumentType());
//            transactionEntity.setExchangeRate(transactionRequest.getExchangeRate());
//            transactionEntity.setDescriptionPda(transactionRequest.getDescriptionPda());
//            transactionEntity.setCurrency(transactionRequest.getCurrency());
//
//
//            BigDecimal totalCredits = transactionRequest.getDetail().stream()
//                    .filter(detail -> Motion.C.equals(detail.getMotion()))
//                    .map(TransactionDetailRequest::getAmount)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//            BigDecimal totalDebits = transactionRequest.getDetail().stream()
//                    .filter(detail -> Motion.D.equals(detail.getMotion()))
//                    .map(TransactionDetailRequest::getAmount)
//                    .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//
//            if (!totalCredits.equals(totalDebits)) {
//                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The values entered in the detail are not balanced");
//            }
//
//            List<TransactionDetailEntity> detail = transactionRequest.getDetail().stream().map(detailRequest -> {
//                AccountEntity account = iAccountRepository.findById(detailRequest.getAccountId())
//                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account with id " + detailRequest.getAccountId() + Constant.NOT_FOUND));
//                TransactionDetailEntity dto = new TransactionDetailEntity();
//                dto.setTransaction(transactionEntity);
//                dto.setAccount(account);
//                dto.setAmount(detailRequest.getAmount());
//                dto.setMotion(detailRequest.getMotion());
//                return dto;
//            }).collect(Collectors.toList());
//
//
//            transactionEntity.setTransactionDetail(detail);
//
//            return transactionRepository.save(transactionEntity);
//        } catch (Exception e) {
//            throw new RuntimeException("Error transaction account: " + e.getMessage());
//        }
    }


    @Transactional
    public TransactionResponse UpdateTransaction(Long id, TransactionRequest transactionRequest) {
        logger.info("Updating transaction with ID: {}", id);

        TransactionEntity existingTransaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("No transaction found with ID: %d", id)));
        //validate transactions
        validateTransactionDetail(transactionRequest.getDetail());
        //update transaction detail
        //get all keys
        Map<Long, TransactionDetailEntity> existingDetailMap = existingTransaction
                .getTransactionDetail().stream()
                .collect(Collectors
                        .toMap(TransactionDetailEntity::getId, detail -> detail));
        /* object that will be used to update the existing details
         *  if the detail is not found in the existing details, it will be added
         *  if the detail is found in the existing details, it will be updated*/
        List<TransactionDetailEntity> updatedDetails = new ArrayList<>();
        //prepared accounts
        Map<Long, AccountEntity> accountsMap = iAccountRepository.findAll().stream().collect(Collectors.toMap(AccountEntity::getId, account -> account));

        // loop over request
        for (TransactionDetailRequest detailRequest : transactionRequest.getDetail()) {
            TransactionDetailEntity detailEntity;
            // check if the detail is in the existing details
            if (detailRequest.getId() != null && existingDetailMap.containsKey(detailRequest.getId())) {
                detailEntity = existingDetailMap.get(detailRequest.getId());
                existingDetailMap.remove(detailRequest.getId()); // remove from list
            } else {
                detailEntity = new TransactionDetailEntity();
                detailEntity.setTransaction(existingTransaction);
            }
            //update values
            detailEntity.setAmount(detailRequest.getAmount());
            detailEntity.setMotion(detailRequest.getMotion());

            //accounts references
            AccountEntity accountEntity = accountsMap.get(detailRequest.getAccountId());
            if (accountEntity == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("account id %d not found, in transaction detail", detailRequest.getId()));
            }
            detailEntity.setAccount(accountEntity);
            updatedDetails.add(detailEntity);
        }
        //delete details that are not in list
        existingTransaction.getTransactionDetail().removeAll(existingDetailMap.values());
        // update list
        for (TransactionDetailEntity detail : updatedDetails) {
            if (!existingTransaction.getTransactionDetail().contains(detail)) {
                existingTransaction.getTransactionDetail().add(detail);
            }
        }
        transactionRepository.save(existingTransaction);
        return entityToResponse(existingTransaction);

//        existingTransaction.setCreateAtDate(transactionRequest.getCreateAtDate());
//        existingTransaction.setReference(transactionRequest.getReference());
//        //  existingTransaction.setDocumentType(transactionRequest.getDocumentType());
//        existingTransaction.setExchangeRate(transactionRequest.getExchangeRate());
//        existingTransaction.setDescriptionPda(transactionRequest.getDescriptionPda());
//        //  existingTransaction.setNumberPda(transactionRequest.getNumberPda());
//        existingTransaction.setCurrency(transactionRequest.getCurrency());
//
//        List<TransactionDetailEntity> updatedDetails = new ArrayList<>();
//        if (transactionRequest.getDetail() != null) {
//            for (TransactionDetailRequest detailRequest : transactionRequest.getDetail()) {
//                if (detailRequest.getId() != null) {
//                    TransactionDetailEntity existingDetail = existingTransaction.getTransactionDetail().stream()
//                            .filter(detail -> detail.getId().equals(detailRequest.getId()))
//                            .findFirst()
//                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
//                                    String.format("No detail entity found with ID: %d", detailRequest.getId())));
//
//                    AccountEntity account = iAccountRepository.findById(detailRequest.getAccountId())
//                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
//                                    "Account with ID " + detailRequest.getAccountId() + Constant.NOT_FOUND));
//
//                    existingDetail.setAccount(account);
//                    existingDetail.setAmount(detailRequest.getAmount());
//                    existingDetail.setMotion(detailRequest.getMotion());
//                    updatedDetails.add(existingDetail);
//                } else {
//                    AccountEntity account = iAccountRepository.findById(detailRequest.getAccountId())
//                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
//                                    "Account with ID " + detailRequest.getAccountId() + Constant.NOT_FOUND));
//
//                    TransactionDetailEntity newDetail = new TransactionDetailEntity();
//                    newDetail.setTransaction(existingTransaction);
//                    newDetail.setAccount(account);
//                    newDetail.setAmount(detailRequest.getAmount());
//                    newDetail.setMotion(detailRequest.getMotion());
//                    updatedDetails.add(newDetail);
//                }
//            }
//        }
//
//        existingTransaction.getTransactionDetail().clear();
//        existingTransaction.getTransactionDetail().addAll(updatedDetails);
//
//        return transactionRepository.save(existingTransaction);

    }

    @Transactional
    public void ChangeTransactionStatus(Long transactionId) {
        logger.info("Changing status of transaction with id {}", transactionId);

        TransactionEntity existingTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("No transaction found with ID: %d", transactionId)));
        if (!existingTransaction.getStatus().equals(StatusTransaction.DRAFT)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The transaction is not in draft status");
        }
        existingTransaction.setStatus(StatusTransaction.SUCCESS);
        transactionRepository.save(existingTransaction);
    }


    @Transactional
    public List<BalanceGeneralResponse> GetBalanceGeneral() {
        List<TransactionBalanceGeneralEntity> response = new ArrayList<>();
        Iterable<TransactionBalanceGeneralEntity> data = view.findAll();
        data.forEach(response::add);
        return AppUtil.buildBalanceGeneral(response);
    }

    @Transactional
    public List<TransactionSumViewEntity> GetTrxSum(TransactionByPeriodRequest transactionRequest) {
        return viewSum.FindTrx(transactionRequest.getAccount(), transactionRequest.getInitDate(), transactionRequest.getEndDate());
    }

    private void validateTransactionDetail(List<TransactionDetailRequest> detailRequest) {
        if (detailRequest.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Detail is required");
        }
        //validate credit and debit
        BigDecimal credit = detailRequest.stream()
                .filter(x -> x.getMotion().equals(Motion.C))
                .map(TransactionDetailRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal debit = detailRequest.stream()
                .filter(x -> x.getMotion().equals(Motion.D))
                .map(TransactionDetailRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal operationResult = credit.subtract(debit);

        if (operationResult.compareTo(BigDecimal.ZERO) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The values entered in the detail are not balanced");
        }
    }


    private List<TransactionDetailEntity> detailToEntity(TransactionEntity TransEntity, List<TransactionDetailRequest> detailRequests) {
        try {
            List<TransactionDetailEntity> result = new ArrayList<>();
            List<AccountEntity> accounts = iAccountRepository.findAll();
            for (TransactionDetailRequest detail : detailRequests) {
                TransactionDetailEntity entity = new TransactionDetailEntity();
                // si la cuenta no existe esto truena
                Optional<AccountEntity> currentAccount = accounts.stream().filter(x -> x.getId().equals(detail.getAccountId())).findFirst();
                currentAccount.ifPresent(entity::setAccount);
                entity.setAmount(detail.getAmount());
                entity.setMotion(detail.getMotion());
                entity.setTransaction(TransEntity);
                result.add(entity);
            }
            return result;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account or accounts in detail do not exist");
        }
    }

    private TransactionResponse entityToResponse(TransactionEntity entity) {
        TransactionResponse response = new TransactionResponse();
        response.setId(entity.getId());
        response.setDate(entity.getCreateAtDate());
        response.setReference(entity.getReference());
        response.setDocumentType(entity.getDocument().getId());
        response.setExchangeRate(entity.getExchangeRate());
        response.setDescription(entity.getDescriptionPda());
        response.setEntryNumber(String.valueOf(entity.getNumberPda()));
        response.setCurrency(entity.getCurrency().toString());
        response.setStatus(entity.getStatus().toString());
        response.setDocumentType(entity.getDocument().getId());
        response.setDocumentName(entity.getDocument().getName());
        //fill up detail
        Set<TransactionDetailResponse> detailResponseSet = new HashSet<>();
        for (TransactionDetailEntity detail : entity.getTransactionDetail()) {
            TransactionDetailResponse detailResponse = new TransactionDetailResponse();
            detailResponse.setId(detail.getId());
            detailResponse.setAmount(detail.getAmount());
            detailResponse.setAccountCode(detail.getAccount().getCode());
            detailResponse.setAccountName(detail.getAccount().getDescription());
            detailResponse.setAccountId(detail.getAccount().getId());
            detailResponse.setShortEntryType(detail.getMotion().toString());
            detailResponse.setEntryType(detail.getMotion().equals(Motion.C) ? "Credito" : "Debito");
            detailResponseSet.add(detailResponse);
        }
        response.setTransactionDetails(detailResponseSet);
        return response;
    }


}
