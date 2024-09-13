package com.sti.accounting.services;

import com.sti.accounting.entities.*;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.IAccountRepository;
import com.sti.accounting.repositories.IAccountingAdjustmentsRepository;
import com.sti.accounting.repositories.ITransactionRepository;
import com.sti.accounting.utils.Motion;
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
public class AccountingAdjustmentService {

    private static final Logger logger = LoggerFactory.getLogger(AccountingAdjustmentService.class);


    private final IAccountingAdjustmentsRepository accountingAdjustmentsRepository;
    private final IAccountRepository iAccountRepository;
    private final ITransactionRepository transactionRepository;

    public AccountingAdjustmentService(IAccountingAdjustmentsRepository accountingAdjustmentsRepository, IAccountRepository iAccountRepository, ITransactionRepository transactionRepository) {
        this.accountingAdjustmentsRepository = accountingAdjustmentsRepository;
        this.iAccountRepository = iAccountRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<AccountingAdjustmentResponse> getAllAccountingAdjustments() {
        return accountingAdjustmentsRepository.findAll().stream().map(this::entityToResponse).toList();
    }


    @Transactional
    public AccountingAdjustmentResponse createAdjustment(AccountingAdjustmentRequest accountingAdjustmentRequest) {
        logger.info("creating adjustment");
        AccountingAdjustmentsEntity entity = new AccountingAdjustmentsEntity();

        TransactionEntity transactionEntity = transactionRepository.findById(accountingAdjustmentRequest.getTransactionId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Transaction %d not valid ", accountingAdjustmentRequest.getTransactionId())
                )
        );

        entity.setTransaction(transactionEntity);
        entity.setReference(accountingAdjustmentRequest.getReference());
        entity.setStatus(StatusTransaction.DRAFT);

        //Adjustment detail validations
        validateAdjustmentTranDetail(accountingAdjustmentRequest.getDetailAdjustment());

        List<AdjustmentDetailEntity> adjustmentDetailEntities = detailToEntity(entity, accountingAdjustmentRequest.getDetailAdjustment());
        entity.setAdjustmentDetail(adjustmentDetailEntities);

        accountingAdjustmentsRepository.save(entity);

        return entityToResponse(entity);

    }

    private void validateAdjustmentTranDetail(List<AdjustmentDetailRequest> detailRequest) {
        if (detailRequest.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Detail is required");
        }
        //validate credit and debit
        BigDecimal credit = detailRequest.stream()
                .filter(x -> x.getMotion().equals(Motion.C))
                .map(AdjustmentDetailRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal debit = detailRequest.stream()
                .filter(x -> x.getMotion().equals(Motion.D))
                .map(AdjustmentDetailRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal operationResult = credit.subtract(debit);

        if (operationResult.compareTo(BigDecimal.ZERO) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The values entered in the detail are not balanced");
        }

        // Validate that accountId is not the same for debit and credit
        Set<Long> creditAccountIds = detailRequest.stream()
                .filter(x -> x.getMotion().equals(Motion.C))
                .map(AdjustmentDetailRequest::getAccountId)
                .collect(Collectors.toSet());

        Set<Long> debitAccountIds = detailRequest.stream()
                .filter(x -> x.getMotion().equals(Motion.D))
                .map(AdjustmentDetailRequest::getAccountId)
                .collect(Collectors.toSet());

        creditAccountIds.retainAll(debitAccountIds);
        if (!creditAccountIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account ID cannot be the same for debit and credit");
        }
    }

    private List<AdjustmentDetailEntity> detailToEntity(AccountingAdjustmentsEntity accountingAdjustmentsEntity, List<AdjustmentDetailRequest> detailRequests) {
        try {
            List<AdjustmentDetailEntity> result = new ArrayList<>();
            List<AccountEntity> accounts = iAccountRepository.findAll();
            for (AdjustmentDetailRequest detail : detailRequests) {
                AdjustmentDetailEntity entity = new AdjustmentDetailEntity();
                // si la cuenta no existe esto truena
                Optional<AccountEntity> currentAccount = accounts.stream().filter(x -> x.getId().equals(detail.getAccountId())).findFirst();
                if (currentAccount.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The account with id " + detail.getAccountId() + "does not exist.");
                }
                currentAccount.ifPresent(entity::setAccount);
                entity.setAmount(detail.getAmount());
                entity.setMotion(detail.getMotion());
                entity.setAdjustment(accountingAdjustmentsEntity);
                result.add(entity);
            }
            return result;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account or accounts in detail do not exist");
        }
    }

    private AccountingAdjustmentResponse entityToResponse(AccountingAdjustmentsEntity entity) {
        AccountingAdjustmentResponse response = new AccountingAdjustmentResponse();
        response.setId(entity.getId());
        response.setTransactionId(entity.getTransaction().getId());
        response.setReference(entity.getReference());
        response.setStatus(entity.getStatus().toString());
        //fill up detail
        Set<AdjustmentDetailResponse> detailResponseSet = new HashSet<>();
        for (AdjustmentDetailEntity detail : entity.getAdjustmentDetail()) {
            AdjustmentDetailResponse detailResponse = new AdjustmentDetailResponse();
            detailResponse.setId(detail.getId());
            detailResponse.setAmount(detail.getAmount());
            detailResponse.setAccountCode(detail.getAccount().getCode());
            detailResponse.setAccountName(detail.getAccount().getDescription());
            detailResponse.setAccountId(detail.getAccount().getId());
            detailResponse.setShortEntryType(detail.getMotion().toString());
            detailResponse.setEntryType(detail.getMotion().equals(Motion.C) ? "Credito" : "Debito");
            detailResponseSet.add(detailResponse);
        }
        response.setAdjustmentDetails(detailResponseSet);
        return response;
    }

}
