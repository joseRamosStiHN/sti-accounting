package com.sti.accounting.services;

import com.sti.accounting.entities.*;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.IAccountRepository;
import com.sti.accounting.repositories.IAccountingJournalRepository;
import com.sti.accounting.repositories.ICreditNotesRepository;
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
public class CreditNotesService {

    private static final Logger logger = LoggerFactory.getLogger(CreditNotesService.class);

    private final ICreditNotesRepository creditNotesRepository;
    private final ITransactionRepository transactionRepository;
    private final ControlAccountBalancesService controlAccountBalancesService;
    private final IAccountingJournalRepository accountingJournalRepository;
    private final IAccountRepository iAccountRepository;
    private final AccountingPeriodService accountingPeriodService;
    private final AuthService authService;

    public CreditNotesService(ICreditNotesRepository creditNotesRepository, ITransactionRepository transactionRepository, ControlAccountBalancesService controlAccountBalancesService, IAccountingJournalRepository accountingJournalRepository, IAccountRepository iAccountRepository, AccountingPeriodService accountingPeriodService, AuthService authService) {
        this.creditNotesRepository = creditNotesRepository;
        this.transactionRepository = transactionRepository;
        this.controlAccountBalancesService = controlAccountBalancesService;
        this.accountingJournalRepository = accountingJournalRepository;
        this.iAccountRepository = iAccountRepository;
        this.accountingPeriodService = accountingPeriodService;
        this.authService = authService;
    }


    public List<CreditNotesResponse> getAllCreditNotes() {
        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();
        String tenantId = authService.getTenantId();

        return creditNotesRepository.findAll().stream()
                .filter(creditNote -> creditNote.getAccountingPeriod().equals(activePeriod) && creditNote.getTenantId().equals(tenantId))
                .map(this::entityToResponse)
                .toList();
    }

    public CreditNotesResponse getCreditNoteById(Long id) {
        String tenantId = authService.getTenantId();

        CreditNotesEntity entity = creditNotesRepository.findById(id).filter(creditNotes -> creditNotes.getTenantId().equals(tenantId))
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                String.format("Credit Note with ID %d not found", id)));
        return entityToResponse(entity);
    }

    public List<CreditNotesResponse> getCreditNoteByTransactionId(Long transactionId) {
        String tenantId = authService.getTenantId();

        List<CreditNotesEntity> entity = creditNotesRepository.getCreditNotesByTransactionIdAndTenantId(transactionId, tenantId);
        if (entity.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Transactions with ID %d not found", transactionId));
        }
        return entity.stream()
                .map(this::entityToResponse)
                .toList();
    }

    @Transactional
    public CreditNotesResponse createCreditNote(CreditNotesRequest creditNotesRequest) {
        logger.info("creating credit note");
        CreditNotesEntity entity = new CreditNotesEntity();
        String tenantId = authService.getTenantId();

        TransactionEntity transactionEntity = transactionRepository.findById(creditNotesRequest.getTransactionId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Transaction %d not valid ", creditNotesRequest.getTransactionId())
                )
        );

        AccountingJournalEntity accountingJournal = accountingJournalRepository.findById(creditNotesRequest.getDiaryType()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Diary type %d not valid ", creditNotesRequest.getDiaryType())
                )
        );

        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        entity.setTransaction(transactionEntity);
        entity.setDescriptionNote(creditNotesRequest.getDescriptionNote());
        entity.setAccountingJournal(accountingJournal);
        entity.setCreateAtDate(creditNotesRequest.getCreateAtDate());
        entity.setStatus(StatusTransaction.DRAFT);
        entity.setAccountingPeriod(activePeriod);
        entity.setTenantId(tenantId);
        entity.setCreatedBy(authService.getUsername());

        //Adjustment detail validations
        validateCreditNotesDetail(creditNotesRequest.getDetailNote());

        List<CreditNotesDetailEntity> creditNotesDetailEntities = detailToEntity(entity, creditNotesRequest.getDetailNote());
        entity.setCreditNoteDetail(creditNotesDetailEntities);

        creditNotesRepository.save(entity);

        return entityToResponse(entity);

    }

    @Transactional
    public CreditNotesResponse updateCreditNote(Long id, CreditNotesRequest request) {
        logger.info("Updating credit note id {}", id);

        final String tenantId = authService.getTenantId();

        CreditNotesEntity entity = creditNotesRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        String.format("Credit Note with ID %d not found", id)
                ));

        if (entity.getStatus() != StatusTransaction.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only DRAFT credit notes can be updated"
            );
        }

        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();
        if (entity.getAccountingPeriod() == null ||
                !Objects.equals(entity.getAccountingPeriod().getId(), activePeriod.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The credit note cannot be updated because it is not in the active accounting period"
            );
        }

        if (request.getTransactionId() != null &&
                !Objects.equals(request.getTransactionId(), entity.getTransaction().getId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Changing the related transaction is not allowed"
            );
        }

        if (request.getDescriptionNote() != null) {
            entity.setDescriptionNote(request.getDescriptionNote());
        }

        if (request.getCreateAtDate() != null) {
            entity.setCreateAtDate(request.getCreateAtDate());
        }

        if (request.getDiaryType() != null) {
            AccountingJournalEntity accountingJournal = accountingJournalRepository.findById(request.getDiaryType())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            String.format("Diary type %d not valid ", request.getDiaryType())
                    ));
            entity.setAccountingJournal(accountingJournal);
        }

        if (request.getDetailNote() != null) {

            validateCreditNotesDetail(request.getDetailNote());

            List<CreditNotesDetailEntity> currentDetails = entity.getCreditNoteDetail();
            if (currentDetails == null) {
                currentDetails = new ArrayList<>();
                entity.setCreditNoteDetail(currentDetails);
            }

            Map<Long, CreditNotesDetailEntity> currentById = currentDetails.stream()
                    .filter(d -> d.getId() != null)
                    .collect(Collectors.toMap(CreditNotesDetailEntity::getId, d -> d));

            List<CreditNotesDetailEntity> updatedList = new ArrayList<>();

            for (CreditNotesDetailRequest incoming : request.getDetailNote()) {

                CreditNotesDetailEntity detailEntity;

                if (incoming.getId() != null && currentById.containsKey(incoming.getId())) {
                    detailEntity = currentById.get(incoming.getId());
                } else {
                    detailEntity = new CreditNotesDetailEntity();
                    detailEntity.setCreditNote(entity);
                }

                Optional<AccountEntity> accountOpt = iAccountRepository.findAll().stream()
                        .filter(x -> x.getId().equals(incoming.getAccountId())
                                && tenantId.equals(x.getTenantId()))
                        .findFirst();

                if (accountOpt.isEmpty()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "The account with id " + incoming.getAccountId() + " does not exist."
                    );
                }

                detailEntity.setAccount(accountOpt.get());
                detailEntity.setAmount(incoming.getAmount());
                detailEntity.setMotion(incoming.getMotion());

                updatedList.add(detailEntity);
            }

            currentDetails.clear();
            currentDetails.addAll(updatedList);
        }

        creditNotesRepository.save(entity);

        return entityToResponse(entity);
    }

    @Transactional
    public void changeCreditNoteStatus(List<Long> creditNoteIds) {
        logger.info("Changing status of credit note with id {}", creditNoteIds);

        for (Long creditNoteId : creditNoteIds) {
            CreditNotesEntity existingCreditNote = creditNotesRepository.findById(creditNoteId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            String.format("No credit note found with ID: %d", creditNoteId)));
            if (!existingCreditNote.getStatus().equals(StatusTransaction.DRAFT)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The credit note is not in draft status");
            }
            existingCreditNote.setStatus(StatusTransaction.SUCCESS);
            creditNotesRepository.save(existingCreditNote);
            controlAccountBalancesService.updateControlAccountCreditNotes(existingCreditNote);

        }

    }

    private void validateCreditNotesDetail(List<CreditNotesDetailRequest> detailRequest) {
        if (detailRequest.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Detail is required");
        }
        //validate credit and debit
        BigDecimal credit = detailRequest.stream()
                .filter(x -> x.getMotion().equals(Motion.C))
                .map(CreditNotesDetailRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal debit = detailRequest.stream()
                .filter(x -> x.getMotion().equals(Motion.D))
                .map(CreditNotesDetailRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal operationResult = credit.subtract(debit);

        if (operationResult.compareTo(BigDecimal.ZERO) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The values entered in the detail are not balanced");
        }

        // Validate that accountId is not the same for debit and credit
        Set<Long> creditAccountIds = detailRequest.stream()
                .filter(x -> x.getMotion().equals(Motion.C))
                .map(CreditNotesDetailRequest::getAccountId)
                .collect(Collectors.toSet());

        Set<Long> debitAccountIds = detailRequest.stream()
                .filter(x -> x.getMotion().equals(Motion.D))
                .map(CreditNotesDetailRequest::getAccountId)
                .collect(Collectors.toSet());

        creditAccountIds.retainAll(debitAccountIds);
        if (!creditAccountIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account ID cannot be the same for debit and credit");
        }
    }

    private List<CreditNotesDetailEntity> detailToEntity(CreditNotesEntity creditNotesEntity, List<CreditNotesDetailRequest> detailRequests) {
        try {
            String tenantId = authService.getTenantId();
            List<CreditNotesDetailEntity> result = new ArrayList<>();
            List<AccountEntity> accounts = iAccountRepository.findAll();
            for (CreditNotesDetailRequest detail : detailRequests) {
                CreditNotesDetailEntity entity = new CreditNotesDetailEntity();
                Optional<AccountEntity> currentAccount = accounts.stream().filter(x -> x.getId().equals(detail.getAccountId()) && x.getTenantId().equals(tenantId)).findFirst();
                if (currentAccount.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The account with id " + detail.getAccountId() + "does not exist.");
                }
                currentAccount.ifPresent(entity::setAccount);
                entity.setAmount(detail.getAmount());
                entity.setMotion(detail.getMotion());
                entity.setCreditNote(creditNotesEntity);
                result.add(entity);
            }
            return result;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account or accounts in detail do not exist");
        }
    }

    private CreditNotesResponse entityToResponse(CreditNotesEntity entity) {
        CreditNotesResponse response = new CreditNotesResponse();
        response.setId(entity.getId());
        response.setTransactionId(entity.getTransaction().getId());
        response.setReference("Nota de Crédito");
        response.setDescriptionNote(entity.getDescriptionNote());
        response.setInvoiceNo(entity.getTransaction().getReference());
        response.setNumberPda(String.valueOf(entity.getTransaction().getNumberPda()));
        response.setDiaryType(entity.getAccountingJournal().getId());
        response.setDiaryName(entity.getAccountingJournal().getDiaryName());
        response.setStatus(entity.getStatus().toString());
        response.setDate(entity.getCreateAtDate());
        response.setCreationDate(entity.getCreateAtTime());
        response.setUser(entity.getCreatedBy());
        response.setAccountingPeriodId(entity.getAccountingPeriod().getId());

        //fill up detail
        Set<CreditNotesDetailResponse> detailResponseSet = new HashSet<>();
        for (CreditNotesDetailEntity detail : entity.getCreditNoteDetail()) {
            CreditNotesDetailResponse detailResponse = new CreditNotesDetailResponse();
            detailResponse.setId(detail.getId());
            detailResponse.setAmount(detail.getAmount());
            detailResponse.setAccountCode(detail.getAccount().getCode());
            detailResponse.setAccountName(detail.getAccount().getDescription());
            detailResponse.setAccountId(detail.getAccount().getId());
            detailResponse.setShortEntryType(detail.getMotion().toString());
            detailResponse.setEntryType(detail.getMotion().equals(Motion.C) ? "Credito" : "Debito");
            detailResponseSet.add(detailResponse);
        }
        response.setDetailNote(detailResponseSet);
        return response;
    }

}
