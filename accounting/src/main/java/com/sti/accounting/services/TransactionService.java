package com.sti.accounting.services;

import com.sti.accounting.entities.*;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.IAccountRepository;
import com.sti.accounting.repositories.IAccountingJournalRepository;
import com.sti.accounting.repositories.IDocumentRepository;
import com.sti.accounting.repositories.ITransactionRepository;
import com.sti.accounting.utils.Motion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final ITransactionRepository transactionRepository;
    private final IAccountRepository iAccountRepository;
    private final IDocumentRepository document;
    private final IAccountingJournalRepository accountingJournalRepository;
    private final ControlAccountBalancesService controlAccountBalancesService;
    private final AccountingPeriodService accountingPeriodService;

    public TransactionService(ITransactionRepository transactionRepository, IAccountRepository iAccountRepository,
                              IDocumentRepository document, IAccountingJournalRepository accountingJournalRepository, ControlAccountBalancesService controlAccountBalancesService, AccountingPeriodService accountingPeriodService) {
        this.transactionRepository = transactionRepository;
        this.iAccountRepository = iAccountRepository;
        this.document = document;
        this.accountingJournalRepository = accountingJournalRepository;
        this.controlAccountBalancesService = controlAccountBalancesService;
        this.accountingPeriodService = accountingPeriodService;
    }

    public List<TransactionResponse> getAllTransaction() {
        return transactionRepository.findAll().stream().map(this::entityToResponse).toList();
    }

    public Map<String, List<AccountTransactionDTO>> getTransactionAccountsByActivePeriod() {
        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        List<Object[]> accountTransactionSummary = transactionRepository.getAccountTransactionSummary();

        Map<String, List<AccountTransactionDTO>> transactionDTOMap = new HashMap<>();

        for (Object[] arr : accountTransactionSummary) {
            AccountTransactionDTO dto = createAccountTransactionDTO(arr);
            if (dto.getAccountingPeriodId().equals(activePeriod.getId())) {
                transactionDTOMap.computeIfAbsent(dto.getDescription(), k -> new ArrayList<>()).add(dto);
            }
        }

        return transactionDTOMap;
    }

    private AccountTransactionDTO createAccountTransactionDTO(Object[] arr) {
        AccountTransactionDTO dto = new AccountTransactionDTO();
        dto.setDescription(getValueAsString(arr[0]));
        dto.setCode(getValueAsString(arr[1]));
        dto.setFatherAccount(getValueAsString(arr[2]));
        dto.setDate(getValueAsString(arr[3]));
        dto.setTypeMovement(getValueAsString(arr[4]));
        dto.setMotion(getValueAsString(arr[5]));
        dto.setAmount(getValueAsString(arr[6]));
        dto.setNumberPda(getValueAsString(arr[7]));
        dto.setCategoryName(getValueAsString(arr[8]));
        dto.setAccountingPeriodId(getValueAsLong(arr[9]));
        return dto;
    }

    private String getValueAsString(Object value) {
        return value != null ? value.toString() : "";
    }

    public List<TransactionResponse> getByDocumentType(Long id) {
        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        List<TransactionEntity> transByDocument = transactionRepository.findByDocumentId(id);

        return transByDocument.stream()
                .filter(transaction -> transaction.getAccountingPeriod().equals(activePeriod))
                .map(this::entityToResponse)
                .toList();
    }

    private Long getValueAsLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public List<TransactionResponse> getTransactionByDateRange(LocalDate startDate, LocalDate endDate) {
        logger.trace("Transaction request with startDate {} and endDate {}", startDate, endDate);
        if (startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Invalid date range: start date %s cannot be after end date", startDate));
        }

        return transactionRepository.findByCreateAtDateBetween(startDate, endDate).stream().map(this::entityToResponse).toList();
    }

    public TransactionResponse getById(Long id) {
        TransactionEntity entity = transactionRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                String.format("Transaction not with ID %d not found", id)));
        return entityToResponse(entity);
    }

    @Transactional
    public TransactionResponse createTransaction(TransactionRequest transactionRequest) {
        logger.info("creating transaction");
        TransactionEntity entity = new TransactionEntity();

        // Get Document
        DocumentEntity documentType = document.findById(transactionRequest.getDocumentType())
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                String.format("Document type %d not valid ", transactionRequest.getDocumentType())
                        )
                );

        AccountingJournalEntity accountingJournal = accountingJournalRepository.findById(transactionRequest.getDiaryType()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Diary type %d not valid ", transactionRequest.getDiaryType())
                )
        );


        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        entity.setDocument(documentType);
        entity.setStatus(StatusTransaction.DRAFT);
        entity.setCurrency(transactionRequest.getCurrency());
        entity.setExchangeRate(transactionRequest.getExchangeRate());
        entity.setReference(transactionRequest.getReference());
        entity.setDescriptionPda(transactionRequest.getDescriptionPda());
        entity.setAccountingJournal(accountingJournal);
        entity.setCreateAtDate(transactionRequest.getCreateAtDate());
        entity.setTypeSale(transactionRequest.getTypeSale());
        entity.setCashValue(transactionRequest.getCashValue());
        entity.setCreditValue(transactionRequest.getCreditValue());
        entity.setTypePayment(transactionRequest.getTypePayment());
        entity.setRtn(transactionRequest.getRtn());
        entity.setSupplierName(transactionRequest.getSupplierName());
        entity.setAccountingPeriod(activePeriod);
        //transaction detail validations
        validateTransactionDetail(transactionRequest.getDetail());

        List<TransactionDetailEntity> transactionDetailEntities = detailToEntity(entity, transactionRequest.getDetail());
        entity.setTransactionDetail(transactionDetailEntities);

        transactionRepository.save(entity);

        return entityToResponse(entity);

    }


    @Transactional
    public TransactionResponse updateTransaction(Long id, TransactionRequest transactionRequest) {
        logger.info("Updating transaction with ID: {}", id);

        TransactionEntity existingTransaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("No transaction found with ID: %d", id)));

        DocumentEntity documentType = document.findById(transactionRequest.getDocumentType())
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                String.format("Document type %d not valid ", transactionRequest.getDocumentType())
                        )
                );

        AccountingJournalEntity accountingJournal = accountingJournalRepository.findById(transactionRequest.getDiaryType()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Diary type %d not valid ", transactionRequest.getDiaryType())
                )
        );

        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        //validate transactions
        validateTransactionDetail(transactionRequest.getDetail());
        //update transaction detail
        //get all keys (var existingDetailMap is a Map<Long, TransactionDetailEntity>)
        var existingDetailMap = existingTransaction
                .getTransactionDetail().stream()
                .collect(Collectors
                        .toMap(TransactionDetailEntity::getId, detail -> detail));
        /* object that will be used to update the existing details
         *  if the detail is not found in the existing details, it will be added
         *  if the detail is found in the existing details, it will be updated*/
        List<TransactionDetailEntity> updatedDetails = new ArrayList<>();
        //prepared accounts (accountsMap is a Map<Long, AccountEntity>)
        var accountsMap = iAccountRepository.findAll().stream().collect(Collectors.toMap(AccountEntity::getId, account -> account));

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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("account id %d not found, in transaction detail", detailRequest.getAccountId()));
            }
            detailEntity.setAccount(accountEntity);
            updatedDetails.add(detailEntity);
        }

        existingTransaction.setDocument(documentType);
        existingTransaction.setCurrency(transactionRequest.getCurrency());
        existingTransaction.setExchangeRate(transactionRequest.getExchangeRate());
        existingTransaction.setReference(transactionRequest.getReference());
        existingTransaction.setDescriptionPda(transactionRequest.getDescriptionPda());
        existingTransaction.setAccountingJournal(accountingJournal);
        existingTransaction.setTypeSale(transactionRequest.getTypeSale());
        existingTransaction.setCashValue(transactionRequest.getCashValue());
        existingTransaction.setCreditValue(transactionRequest.getCreditValue());
        existingTransaction.setTypePayment(transactionRequest.getTypePayment());
        existingTransaction.setRtn(transactionRequest.getRtn());
        existingTransaction.setSupplierName(transactionRequest.getSupplierName());
        existingTransaction.setAccountingPeriod(activePeriod);

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


    }

    @Transactional
    public void changeTransactionStatus(List<Long> transactionIds) {
        logger.info("Changing status of transaction with id {}", transactionIds);

        for (Long transactionId : transactionIds) {
            TransactionEntity existingTransaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            String.format("No transaction found with ID: %d", transactionId)));
            if (!existingTransaction.getStatus().equals(StatusTransaction.DRAFT)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The transaction is not in draft status");
            }
            existingTransaction.setStatus(StatusTransaction.SUCCESS);
            transactionRepository.save(existingTransaction);
            controlAccountBalancesService.updateControlAccountBalances(existingTransaction);
        }
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

        // Validate that accountId is not the same for debit and credit
        Set<Long> creditAccountIds = detailRequest.stream()
                .filter(x -> x.getMotion().equals(Motion.C))
                .map(TransactionDetailRequest::getAccountId)
                .collect(Collectors.toSet());

        Set<Long> debitAccountIds = detailRequest.stream()
                .filter(x -> x.getMotion().equals(Motion.D))
                .map(TransactionDetailRequest::getAccountId)
                .collect(Collectors.toSet());

        creditAccountIds.retainAll(debitAccountIds);
        if (!creditAccountIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account ID cannot be the same for debit and credit");
        }
    }


    private List<TransactionDetailEntity> detailToEntity(TransactionEntity transactionEntity, List<TransactionDetailRequest> detailRequests) {
        try {
            List<TransactionDetailEntity> result = new ArrayList<>();
            List<AccountEntity> accounts = iAccountRepository.findAll();
            for (TransactionDetailRequest detail : detailRequests) {
                TransactionDetailEntity entity = new TransactionDetailEntity();
                // si la cuenta no existe esto truena
                Optional<AccountEntity> currentAccount = accounts.stream().filter(x -> x.getId().equals(detail.getAccountId())).findFirst();
                if (currentAccount.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The account with id " + detail.getAccountId() + "does not exist.");
                }
                currentAccount.ifPresent(entity::setAccount);
                entity.setAmount(detail.getAmount());
                entity.setMotion(detail.getMotion());
                entity.setTransaction(transactionEntity);
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
        response.setCreationDate(entity.getCreateAtTime());
        response.setReference(entity.getReference());
        response.setDocumentType(entity.getDocument().getId());
        response.setExchangeRate(entity.getExchangeRate());
        response.setDescription(entity.getDescriptionPda());
        response.setNumberPda(String.valueOf(entity.getNumberPda()));
        response.setCurrency(entity.getCurrency().toString());
        response.setStatus(entity.getStatus().toString());
        response.setDocumentType(entity.getDocument().getId());
        response.setDocumentName(entity.getDocument().getName());
        response.setDiaryType(entity.getAccountingJournal().getId());
        response.setDiaryName(entity.getAccountingJournal().getDiaryName());
        response.setUser("user.mock");
        response.setTypeSale(entity.getTypeSale());
        response.setCashValue(entity.getCashValue());
        response.setCreditValue(entity.getCreditValue());
        response.setTypePayment(entity.getTypePayment());
        response.setRtn(entity.getRtn());
        response.setSupplierName(entity.getSupplierName());
        response.setAccountingPeriodId(entity.getAccountingPeriod().getId());

        //fill up detail
        Set<TransactionDetailResponse> detailResponseSet = new HashSet<>();
        for (TransactionDetailEntity detail : entity.getTransactionDetail()) {
            TransactionDetailResponse detailResponse = new TransactionDetailResponse();
            detailResponse.setId(detail.getId());
            detailResponse.setAmount(detail.getAmount());
            detailResponse.setAccountCode(detail.getAccount().getCode());
            detailResponse.setAccountName(detail.getAccount().getDescription());
            detailResponse.setAccountId(detail.getAccount().getId());
            if (detail.getAccount().getBalances().getFirst() != null) {
                detailResponse.setTypicalBalance(detail.getAccount().getBalances().getFirst().getTypicalBalance());
                detailResponse.setInitialBalance(detail.getAccount().getBalances().getFirst().getInitialBalance());
            }
            detailResponse.setShortEntryType(detail.getMotion().toString());
            detailResponse.setEntryType(detail.getMotion().equals(Motion.C) ? "Credito" : "Debito");
            detailResponseSet.add(detailResponse);
        }
        response.setTransactionDetails(detailResponseSet);
        return response;
    }


}
