package com.sti.accounting.services;

import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.entities.AccountTypeEntity;
import com.sti.accounting.entities.AccountingJournalEntity;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.IAccountRepository;
import com.sti.accounting.repositories.IAccountTypeRepository;
import com.sti.accounting.repositories.IAccountingJournalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class AccountingJournalService {

    private static final Logger logger = LoggerFactory.getLogger(AccountingJournalService.class);
    private final IAccountingJournalRepository accountingJournalRepository;
    private final IAccountTypeRepository accountTypeRepository;
    private final IAccountRepository iAccountRepository;

    public AccountingJournalService(IAccountingJournalRepository accountingJournalRepository, IAccountTypeRepository accountTypeRepository, IAccountRepository iAccountRepository) {
        this.accountingJournalRepository = accountingJournalRepository;
        this.accountTypeRepository = accountTypeRepository;
        this.iAccountRepository = iAccountRepository;

    }

    public List<AccountingJournalResponse> getAllAccountingJournal() {
        return this.accountingJournalRepository.findAll().stream().map(this::toResponse).toList();
    }

    public AccountingJournalResponse getAccountingJournalById(Long id) {
        logger.trace("accounting journal request with id {}", id);
        AccountingJournalEntity accountingJournalEntity = accountingJournalRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("No accounting journal were found with the id %s", id))
        );
        return toResponse(accountingJournalEntity);
    }

    public AccountingJournalResponse createAccountingJournal(AccountingJournalRequest accountingJournalRequest) {
        AccountingJournalEntity entity = new AccountingJournalEntity();


        entity.setDiaryName(accountingJournalRequest.getDiaryName());
        Long accountTypeId = accountingJournalRequest.getAccountType().longValue();
        AccountTypeEntity accountTypeEntity = accountTypeRepository.findById(accountTypeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Account Type"));
        entity.setAccountType(accountTypeEntity);
        entity.setDefaultIncomeAccount(findAndAssignAccount(accountingJournalRequest.getDefaultIncomeAccount(), "Invalid Default Income Account"));
        entity.setDefaultExpenseAccount(findAndAssignAccount(accountingJournalRequest.getDefaultExpenseAccount(), "Invalid Default Expense Account"));
        entity.setCashAccount(findAndAssignAccount(accountingJournalRequest.getCashAccount(), "Invalid Cash Account"));
        entity.setLossAccount(findAndAssignAccount(accountingJournalRequest.getLossAccount(), "Invalid Loss Account"));
        entity.setTransitAccount(findAndAssignAccount(accountingJournalRequest.getTransitAccount(), "Invalid Transit Account"));
        entity.setProfitAccount(findAndAssignAccount(accountingJournalRequest.getProfitAccount(), "Invalid Profit Account"));
        entity.setBankAccount(findAndAssignAccount(accountingJournalRequest.getBankAccount(), "Invalid Bank Account"));
        entity.setAccountNumber(accountingJournalRequest.getAccountNumber());
        entity.setDefaultAccount(findAndAssignAccount(accountingJournalRequest.getDefaultAccount(), "Invalid Default Account"));
        entity.setCode(accountingJournalRequest.getCode());
        entity.setStatus(false);
        accountingJournalRepository.save(entity);

        return toResponse(entity);
    }

    public AccountingJournalResponse updateAccountingJournal(Long id, AccountingJournalRequest accountingJournalRequest) {

        logger.info("Updating accounting journal with ID: {}", id);

        AccountingJournalEntity existingAccountingJournal = accountingJournalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("No accounting journal found with ID: %d", id)));

        existingAccountingJournal.setDiaryName(accountingJournalRequest.getDiaryName());
        Long accountTypeId = accountingJournalRequest.getAccountType().longValue();
        AccountTypeEntity accountTypeEntity = accountTypeRepository.findById(accountTypeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Account Type"));
        existingAccountingJournal.setAccountType(accountTypeEntity);
        existingAccountingJournal.setDefaultIncomeAccount(findAndAssignAccount(accountingJournalRequest.getDefaultIncomeAccount(), "Invalid Default Income Account"));
        existingAccountingJournal.setDefaultExpenseAccount(findAndAssignAccount(accountingJournalRequest.getDefaultExpenseAccount(), "Invalid Default Expense Account"));
        existingAccountingJournal.setCashAccount(findAndAssignAccount(accountingJournalRequest.getCashAccount(), "Invalid Cash Account"));
        existingAccountingJournal.setLossAccount(findAndAssignAccount(accountingJournalRequest.getLossAccount(), "Invalid Loss Account"));
        existingAccountingJournal.setTransitAccount(findAndAssignAccount(accountingJournalRequest.getTransitAccount(), "Invalid Transit Account"));
        existingAccountingJournal.setProfitAccount(findAndAssignAccount(accountingJournalRequest.getProfitAccount(), "Invalid Profit Account"));
        existingAccountingJournal.setBankAccount(findAndAssignAccount(accountingJournalRequest.getBankAccount(), "Invalid Bank Account"));
        existingAccountingJournal.setAccountNumber(accountingJournalRequest.getAccountNumber());
        existingAccountingJournal.setDefaultAccount(findAndAssignAccount(accountingJournalRequest.getDefaultAccount(), "Invalid Default Account"));
        existingAccountingJournal.setCode(accountingJournalRequest.getCode());
        existingAccountingJournal.setStatus(accountingJournalRequest.isStatus());
        accountingJournalRepository.save(existingAccountingJournal);

        return toResponse(existingAccountingJournal);
    }

    private AccountEntity findAndAssignAccount(Long accountId, String errorMessage) {
        if (accountId != null) {
            return iAccountRepository.findById(accountId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage));
        }
        return null;
    }

    public String getDiaryName(Long diaryId) {
        AccountingJournalEntity accountingJournalEntity = accountingJournalRepository.findById(diaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("No accounting journal were found with the id %s", diaryId)));
        return accountingJournalEntity.getDiaryName();
    }

    private AccountingJournalResponse toResponse(AccountingJournalEntity entity) {
        AccountingJournalResponse response = new AccountingJournalResponse();

        response.setId(entity.getId());
        response.setDiaryName(entity.getDiaryName());
        response.setAccountTypeName(entity.getAccountType().getName());
        response.setAccountType(entity.getAccountType().getId());
        if (entity.getDefaultIncomeAccount() != null) {
            response.setDefaultIncomeAccount(entity.getDefaultIncomeAccount().getId());
            response.setDefaultIncomeAccountName(entity.getDefaultIncomeAccount().getDescription());
            response.setDefaultIncomeAccountCode(entity.getDefaultIncomeAccount().getCode());
        }

        if (entity.getDefaultExpenseAccount() != null) {
            response.setDefaultExpenseAccount(entity.getDefaultExpenseAccount().getId());
            response.setDefaultExpenseAccountName(entity.getDefaultExpenseAccount().getDescription());
            response.setDefaultExpenseAccountCode(entity.getDefaultExpenseAccount().getCode());

        }

        if (entity.getCashAccount() != null) {
            response.setCashAccount(entity.getCashAccount().getId());
            response.setCashAccountName(entity.getCashAccount().getDescription());
            response.setCashAccountCode(entity.getCashAccount().getCode());
        }

        if (entity.getLossAccount() != null) {
            response.setLossAccount(entity.getLossAccount().getId());
            response.setLossAccountName(entity.getLossAccount().getDescription());
            response.setLossAccountCode(entity.getLossAccount().getCode());
        }
        if (entity.getTransitAccount() != null) {
            response.setTransitAccount(entity.getTransitAccount().getId());
            response.setTransitAccountName(entity.getTransitAccount().getDescription());
            response.setTransitAccountCode(entity.getTransitAccount().getCode());
        }
        if (entity.getProfitAccount() != null) {
            response.setProfitAccount(entity.getProfitAccount().getId());
            response.setProfitAccountName(entity.getProfitAccount().getDescription());
            response.setProfitAccountCode(entity.getProfitAccount().getCode());
        }

        if (entity.getBankAccount() != null) {
            response.setBankAccount(entity.getBankAccount().getId());
            response.setBankAccountName(entity.getBankAccount().getDescription());
            response.setBankAccountCode(entity.getBankAccount().getCode());
        }
        response.setAccountNumber(entity.getAccountNumber());

        if (entity.getDefaultAccount() != null) {
            response.setDefaultAccount(entity.getDefaultAccount().getId());
            response.setDefaultAccountName(entity.getDefaultAccount().getDescription());
            response.setDefaultAccountCode(entity.getDefaultAccount().getCode());
        }

        response.setCode(entity.getCode());
        response.setStatus(entity.isStatus());
        response.setCreateDate(entity.getCreateDate());
        return response;
    }
}
