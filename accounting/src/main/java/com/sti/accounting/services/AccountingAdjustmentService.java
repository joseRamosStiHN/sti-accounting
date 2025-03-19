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
    private final ControlAccountBalancesService controlAccountBalancesService;
    private final AccountingPeriodService accountingPeriodService;
    private final AuthService authService;

    public AccountingAdjustmentService(IAccountingAdjustmentsRepository accountingAdjustmentsRepository, IAccountRepository iAccountRepository, ITransactionRepository transactionRepository, ControlAccountBalancesService controlAccountBalancesService, AccountingPeriodService accountingPeriodService, AuthService authService) {
        this.accountingAdjustmentsRepository = accountingAdjustmentsRepository;
        this.iAccountRepository = iAccountRepository;
        this.transactionRepository = transactionRepository;
        this.controlAccountBalancesService = controlAccountBalancesService;
        this.accountingPeriodService = accountingPeriodService;
        this.authService = authService;
    }

    public List<AccountingAdjustmentResponse> getAllAccountingAdjustments() {
        logger.info("Getting all accounting adjustments");
        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();
        String tenantId = authService.getTenantId();

        return accountingAdjustmentsRepository.findByTenantIdAndAccountingPeriodId(tenantId, activePeriod.getId())
                .stream()
                .map(this::entityToResponse)
                .toList();
    }

    public AccountingAdjustmentResponse getAccountingAdjustmentsById(Long id) {
        logger.info("Getting accounting adjustments by id: {}", id);
        String tenantId = authService.getTenantId();
        AccountingAdjustmentsEntity entity = accountingAdjustmentsRepository.findByTenantIdAndId(tenantId, id);

        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Accounting adjustments with ID %d not found", id));
        }

        return entityToResponse(entity);
    }

    public List<AccountingAdjustmentResponse> getAccountingAdjustmentsByTransactionId(Long transactionId) {
        logger.info("Getting accounting adjustments by transaction id: {}", transactionId);
        String tenantId = authService.getTenantId();

        List<AccountingAdjustmentsEntity> entity = accountingAdjustmentsRepository.getAccountingAdjustmentsByTransactionIdAndTenantId(transactionId, tenantId);
        if (entity.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Transactions with ID %d not found", transactionId));
        }
        return entity.stream()
                .map(this::entityToResponse)
                .toList();
    }

    @Transactional
    public AccountingAdjustmentResponse createAdjustment(AccountingAdjustmentRequest accountingAdjustmentRequest) {
        logger.info("creating adjustment");
        AccountingAdjustmentsEntity entity = new AccountingAdjustmentsEntity();
        String tenantId = authService.getTenantId();

        TransactionEntity transactionEntity = transactionRepository.findById(accountingAdjustmentRequest.getTransactionId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Transaction %d not valid ", accountingAdjustmentRequest.getTransactionId())
                )
        );

        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        entity.setTransaction(transactionEntity);
        entity.setReference(accountingAdjustmentRequest.getReference());
        entity.setDescriptionAdjustment(accountingAdjustmentRequest.getDescriptionAdjustment());
        entity.setStatus(StatusTransaction.DRAFT);
        entity.setAccountingPeriod(activePeriod);
        entity.setTenantId(tenantId);

        //Adjustment detail validations
        validateAdjustmentTranDetail(accountingAdjustmentRequest.getDetailAdjustment());

        List<AdjustmentDetailEntity> adjustmentDetailEntities = detailToEntity(entity, accountingAdjustmentRequest.getDetailAdjustment());
        entity.setAdjustmentDetail(adjustmentDetailEntities);

        accountingAdjustmentsRepository.save(entity);

        return entityToResponse(entity);

    }

    @Transactional
    public void changeAdjustmentStatus(List<Long> adjustmentIds) {
        logger.info("Changing status of adjustment with id {}", adjustmentIds);

        List<AccountingAdjustmentsEntity> adjustments = accountingAdjustmentsRepository.findByIdInAndStatus(adjustmentIds, StatusTransaction.DRAFT);

        if (adjustments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No adjustment found with the provided IDs");
        }

        adjustments.forEach(adjustment -> {
            adjustment.setStatus(StatusTransaction.SUCCESS);
            controlAccountBalancesService.updateControlAccountBalancesAdjustment(adjustment);
        });

        accountingAdjustmentsRepository.saveAll(adjustments);
    }

    private void validateAdjustmentTranDetail(List<AdjustmentDetailRequest> detailRequest) {
        logger.info("Validating adjustment detail");
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
        logger.info("Converting detail to entity");
        try {
            String tenantId = authService.getTenantId();
            List<AdjustmentDetailEntity> result = new ArrayList<>();
            List<AccountEntity> accounts = iAccountRepository.findAll();
            for (AdjustmentDetailRequest detail : detailRequests) {
                AdjustmentDetailEntity entity = new AdjustmentDetailEntity();
                Optional<AccountEntity> currentAccount = accounts.stream().filter(x -> x.getId().equals(detail.getAccountId()) && x.getTenantId().equals(tenantId)).findFirst();
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
            logger.error("Error in detailToEntity: {}", e.getMessage());
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
        response.setDiaryType(entity.getTransaction().getAccountingJournal().getId());
        response.setDiaryName(entity.getTransaction().getAccountingJournal().getDiaryName());
        response.setNumberPda(String.valueOf(entity.getTransaction().getNumberPda()));
        response.setStatus(entity.getStatus().toString());
        response.setCreationDate(entity.getCreationDate());
        response.setUser("user.mock");
        response.setAccountingPeriodId(entity.getAccountingPeriod().getId());

        Set<AdjustmentDetailResponse> detailResponseSet = new HashSet<>();
        for (AdjustmentDetailEntity detail : entity.getAdjustmentDetail()) {
            AdjustmentDetailResponse detailResponse = new AdjustmentDetailResponse();
            detailResponse.setId(detail.getId());
            detailResponse.setAmount(detail.getAmount());
            detailResponse.setAccountCode(detail.getAccount().getCode());
            detailResponse.setAccountName(detail.getAccount().getDescription());
            detailResponse.setAccountId(detail.getAccount().getId());
//            if (detail.getAccount().getBalances() != null) {
//                detailResponse.setTypicalBalance(detail.getAccount().getBalances().getFirst().getTypicalBalance());
//                detailResponse.setInitialBalance(detail.getAccount().getBalances().getFirst().getInitialBalance());
//            }
            detailResponse.setShortEntryType(detail.getMotion().toString());
            detailResponse.setEntryType(detail.getMotion().equals(Motion.C) ? "Credito" : "Debito");
            detailResponseSet.add(detailResponse);
        }
        response.setAdjustmentDetails(detailResponseSet);
        return response;
    }

}
