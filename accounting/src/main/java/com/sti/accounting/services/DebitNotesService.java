package com.sti.accounting.services;

import com.sti.accounting.entities.*;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.IAccountRepository;
import com.sti.accounting.repositories.IAccountingJournalRepository;
import com.sti.accounting.repositories.IDebitNotesRepository;
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
public class DebitNotesService {

    private static final Logger logger = LoggerFactory.getLogger(DebitNotesService.class);

    private final IDebitNotesRepository debitNotesRepository;
    private final ITransactionRepository transactionRepository;
    private final ControlAccountBalancesService controlAccountBalancesService;
    private final IAccountingJournalRepository accountingJournalRepository;
    private final IAccountRepository iAccountRepository;

    public DebitNotesService(IDebitNotesRepository debitNotesRepository, ITransactionRepository transactionRepository, ControlAccountBalancesService controlAccountBalancesService, IAccountingJournalRepository accountingJournalRepository, IAccountRepository iAccountRepository) {
        this.debitNotesRepository = debitNotesRepository;
        this.transactionRepository = transactionRepository;
        this.controlAccountBalancesService = controlAccountBalancesService;
        this.accountingJournalRepository = accountingJournalRepository;
        this.iAccountRepository = iAccountRepository;
    }

    public List<DebitNotesResponse> getAllDebitNotes() {
        return debitNotesRepository.findAll().stream().map(this::entityToResponse).toList();
    }

    public DebitNotesResponse getDebitNoteById(Long id) {
        DebitNotesEntity entity = debitNotesRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                String.format("Debit Note with ID %d not found", id)));
        return entityToResponse(entity);
    }

    public List<DebitNotesResponse> getDebitNoteByTransactionId(Long transactionId) {
        List<DebitNotesEntity> entity = debitNotesRepository.getDebitNotesByTransactionId(transactionId);
        if (entity.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Transactions with ID %d not found", transactionId));
        }
        return entity.stream()
                .map(this::entityToResponse)
                .toList();
    }

    @Transactional
    public DebitNotesResponse createDebitNote(DebitNotesRequest debitNotesRequest) {
        logger.info("creating debit note");
        DebitNotesEntity entity = new DebitNotesEntity();

        TransactionEntity transactionEntity = transactionRepository.findById(debitNotesRequest.getTransactionId()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Transaction %d not valid ", debitNotesRequest.getTransactionId())
                )
        );

        AccountingJournalEntity accountingJournal = accountingJournalRepository.findById(debitNotesRequest.getDiaryType()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("Diary type %d not valid ", debitNotesRequest.getDiaryType())
                )
        );

        entity.setTransaction(transactionEntity);
        entity.setDescriptionNote(debitNotesRequest.getDescriptionNote());
        entity.setAccountingJournal(accountingJournal);
        entity.setCreateAtDate(debitNotesRequest.getCreateAtDate());
        entity.setStatus(StatusTransaction.DRAFT);

        //Adjustment detail validations
        validateDebitNotesDetail(debitNotesRequest.getDetailNote());

        List<DebitNotesDetailEntity> debitNotesDetailEntities = detailToEntity(entity, debitNotesRequest.getDetailNote());
        entity.setDebitNoteDetail(debitNotesDetailEntities);

        debitNotesRepository.save(entity);

        return entityToResponse(entity);

    }

    @Transactional
    public void changeDebitNoteStatus(Long debitNoteId) {
        logger.info("Changing status of debit note with id {}", debitNoteId);

        DebitNotesEntity existingDebitNote = debitNotesRepository.findById(debitNoteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("No debit note found with ID: %d", debitNoteId)));
        if (!existingDebitNote.getStatus().equals(StatusTransaction.DRAFT)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The debit note is not in draft status");
        }
        existingDebitNote.setStatus(StatusTransaction.SUCCESS);
        debitNotesRepository.save(existingDebitNote);
        controlAccountBalancesService.updateControlAccountDebitNotes(existingDebitNote);

    }

    private void validateDebitNotesDetail(List<DebitNotesDetailRequest> detailRequest) {
        if (detailRequest.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Detail is required");
        }
        //validate credit and debit
        BigDecimal credit = detailRequest.stream()
                .filter(x -> x.getMotion().equals(Motion.C))
                .map(DebitNotesDetailRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal debit = detailRequest.stream()
                .filter(x -> x.getMotion().equals(Motion.D))
                .map(DebitNotesDetailRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal operationResult = credit.subtract(debit);

        if (operationResult.compareTo(BigDecimal.ZERO) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The values entered in the detail are not balanced");
        }

        // Validate that accountId is not the same for debit and credit
        Set<Long> creditAccountIds = detailRequest.stream()
                .filter(x -> x.getMotion().equals(Motion.C))
                .map(DebitNotesDetailRequest::getAccountId)
                .collect(Collectors.toSet());

        Set<Long> debitAccountIds = detailRequest.stream()
                .filter(x -> x.getMotion().equals(Motion.D))
                .map(DebitNotesDetailRequest::getAccountId)
                .collect(Collectors.toSet());

        creditAccountIds.retainAll(debitAccountIds);
        if (!creditAccountIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account ID cannot be the same for debit and credit");
        }
    }

    private List<DebitNotesDetailEntity> detailToEntity(DebitNotesEntity debitNotesEntity, List<DebitNotesDetailRequest> detailRequests) {
        try {
            List<DebitNotesDetailEntity> result = new ArrayList<>();
            List<AccountEntity> accounts = iAccountRepository.findAll();
            for (DebitNotesDetailRequest detail : detailRequests) {
                DebitNotesDetailEntity entity = new DebitNotesDetailEntity();
                Optional<AccountEntity> currentAccount = accounts.stream().filter(x -> x.getId().equals(detail.getAccountId())).findFirst();
                if (currentAccount.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The account with id " + detail.getAccountId() + "does not exist.");
                }
                currentAccount.ifPresent(entity::setAccount);
                entity.setAmount(detail.getAmount());
                entity.setMotion(detail.getMotion());
                entity.setDebitNote(debitNotesEntity);
                result.add(entity);
            }
            return result;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Account or accounts in detail do not exist");
        }
    }


    private DebitNotesResponse entityToResponse(DebitNotesEntity entity) {
        DebitNotesResponse response = new DebitNotesResponse();
        response.setId(entity.getId());
        response.setTransactionId(entity.getTransaction().getId());
        response.setReference("Nota de Débito");
        response.setDescriptionNote(entity.getDescriptionNote());
        response.setInvoiceNo(entity.getTransaction().getReference());
        response.setNumberPda(String.valueOf(entity.getTransaction().getNumberPda()));
        response.setDiaryType(entity.getAccountingJournal().getId());
        response.setDiaryName(entity.getAccountingJournal().getDiaryName());
        response.setStatus(entity.getStatus().toString());
        response.setDate(entity.getCreateAtDate());
        response.setCreationDate(entity.getCreateAtTime());
        response.setUser("user.mock");
        //fill up detail
        Set<DebitNotesDetailResponse> detailResponseSet = new HashSet<>();
        for (DebitNotesDetailEntity detail : entity.getDebitNoteDetail()) {
            DebitNotesDetailResponse detailResponse = new DebitNotesDetailResponse();
            detailResponse.setId(detail.getId());
            detailResponse.setAmount(detail.getAmount());
            detailResponse.setAccountCode(detail.getAccount().getCode());
            detailResponse.setAccountName(detail.getAccount().getDescription());
            detailResponse.setAccountId(detail.getAccount().getId());
            detailResponse.setTypicalBalance(detail.getAccount().getBalances().getFirst().getTypicalBalance());
            detailResponse.setInitialBalance(detail.getAccount().getBalances().getFirst().getInitialBalance());
            detailResponse.setShortEntryType(detail.getMotion().toString());
            detailResponse.setEntryType(detail.getMotion().equals(Motion.C) ? "Credito" : "Debito");
            detailResponseSet.add(detailResponse);
        }
        response.setDetailNote(detailResponseSet);
        return response;
    }

}
