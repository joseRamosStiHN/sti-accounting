package com.sti.accounting.services;

import com.sti.accounting.entities.*;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.IAccountRepository;
import com.sti.accounting.repositories.IAccountingAdjustmentsRepository;
import com.sti.accounting.repositories.ITransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

@Service
public class AccountingAdjustmentService {

    private static final Logger logger = LoggerFactory.getLogger(AccountingAdjustmentService.class);


    private final IAccountingAdjustmentsRepository accountingAdjustmentsRepository;
    private final IAccountRepository iAccountRepository;
    private final ITransactionRepository transactionRepository;
    private final ControlAccountBalancesService controlAccountBalancesService;

    public AccountingAdjustmentService(IAccountingAdjustmentsRepository accountingAdjustmentsRepository, IAccountRepository iAccountRepository, ITransactionRepository transactionRepository, ControlAccountBalancesService controlAccountBalancesService) {
        this.accountingAdjustmentsRepository = accountingAdjustmentsRepository;
        this.iAccountRepository = iAccountRepository;
        this.transactionRepository = transactionRepository;
        this.controlAccountBalancesService = controlAccountBalancesService;
    }

    public List<AccountingAdjustmentResponse> getAllAccountingAdjustments() {
        return accountingAdjustmentsRepository.findAll().stream().map(this::entityToResponse).toList();
    }

    public AccountingAdjustmentResponse getAccountingAdjustmentsById(Long id) {
        AccountingAdjustmentsEntity entity = accountingAdjustmentsRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                String.format("Accounting adjustments not with ID %d not found", id)));
        return entityToResponse(entity);
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
        entity.setDescriptionAdjustment(accountingAdjustmentRequest.getDescriptionAdjustment());
        entity.setStatus(StatusTransaction.DRAFT);

        //Adjustment detail validations
        validateAdjustmentTranDetail(accountingAdjustmentRequest.getDetailAdjustment());

        List<AdjustmentDetailEntity> adjustmentDetailEntities = detailToEntity(entity, accountingAdjustmentRequest.getDetailAdjustment());
        entity.setAdjustmentDetail(adjustmentDetailEntities);

        accountingAdjustmentsRepository.save(entity);

        return entityToResponse(entity);

    }

    @Transactional
    public void changeAdjustmentStatus(Long adjustmentId) {
        logger.info("Changing status of adjustment with id {}", adjustmentId);

        AccountingAdjustmentsEntity existingAdjustment = accountingAdjustmentsRepository.findById(adjustmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No adjustment found with ID: " + adjustmentId));

        if (!existingAdjustment.getStatus().equals(StatusTransaction.DRAFT)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The adjustment is not in draft status");
        }

        existingAdjustment.setStatus(StatusTransaction.SUCCESS);

        accountingAdjustmentsRepository.save(existingAdjustment);
        controlAccountBalancesService.updateControlAccountBalancesAdjustment(existingAdjustment);

    }

    private void validateAdjustmentTranDetail(List<AdjustmentDetailRequest> detailRequest) {
        if (detailRequest.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Detail is required");
        }
        //validate credit and debit
        BigDecimal credit = detailRequest.stream()
                .map(AdjustmentDetailRequest::getCredit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal debit = detailRequest.stream()
                .map(AdjustmentDetailRequest::getDebit)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal operationResult = credit.subtract(debit);

        if (operationResult.compareTo(BigDecimal.ZERO) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The values entered in the detail are not balanced");
        }

    }

    private List<AdjustmentDetailEntity> detailToEntity(AccountingAdjustmentsEntity accountingAdjustmentsEntity, List<AdjustmentDetailRequest> detailRequests) {
        try {
            List<AdjustmentDetailEntity> result = new ArrayList<>();
            List<AccountEntity> accounts = iAccountRepository.findAll();
            for (AdjustmentDetailRequest detail : detailRequests) {
                AdjustmentDetailEntity entity = new AdjustmentDetailEntity();
                Optional<AccountEntity> currentAccount = accounts.stream().filter(x -> x.getId().equals(detail.getAccountId())).findFirst();
                if (currentAccount.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The account with id " + detail.getAccountId() + "does not exist.");
                }
                currentAccount.ifPresent(entity::setAccount);
                entity.setDebit(detail.getDebit());
                entity.setCredit(detail.getCredit());
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
        response.setDescriptionAdjustment(entity.getDescriptionAdjustment());
        response.setInvoiceNo(entity.getTransaction().getReference());
        response.setDiaryName(entity.getTransaction().getAccountingJournal().getDiaryName());
        response.setNumberPda(String.valueOf(entity.getTransaction().getNumberPda()));
        response.setStatus(entity.getStatus().toString());
        response.setCreationDate(entity.getCreationDate());

        Set<AdjustmentDetailResponse> detailResponseSet = new HashSet<>();
        for (AdjustmentDetailEntity detail : entity.getAdjustmentDetail()) {
            AdjustmentDetailResponse detailResponse = new AdjustmentDetailResponse();
            detailResponse.setId(detail.getId());
            detailResponse.setDebit(detail.getDebit());
            detailResponse.setCredit(detail.getCredit());
            detailResponse.setAccountCode(detail.getAccount().getCode());
            detailResponse.setAccountName(detail.getAccount().getDescription());
            detailResponse.setAccountId(detail.getAccount().getId());
            detailResponse.setTypicalBalance(detail.getAccount().getBalances().getFirst().getTypicalBalance());
            detailResponse.setInitialBalance(detail.getAccount().getBalances().getFirst().getInitialBalance());
            detailResponseSet.add(detailResponse);
        }
        response.setAdjustmentDetails(detailResponseSet);
        return response;
    }

}
