package com.sti.accounting.services;

import com.sti.accounting.entities.AccountCategoryEntity;
import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.models.AccountBalance;
import com.sti.accounting.models.AccountCategory;
import com.sti.accounting.models.AccountRequest;
import com.sti.accounting.models.AccountResponse;
import com.sti.accounting.repositories.IAccountCategoryRepository;
import com.sti.accounting.repositories.IAccountRepository;
import com.sti.accounting.utils.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;


@Service
public class AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private final IAccountRepository iAccountRepository;
    private final IAccountCategoryRepository categoryRepository;

    public AccountService(IAccountRepository iAccountRepository, IAccountCategoryRepository categoryRepository) {
        this.iAccountRepository = iAccountRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<AccountResponse> getAllAccount() {
       return this.iAccountRepository.findAll().stream().map(this::toResponse).toList();
    }

    public AccountResponse getById(Long id) {
        logger.trace("account request with id {}", id);
        AccountEntity accountEntity = iAccountRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,String.format("No account were found with the id %s", id))
        );
       return toResponse(accountEntity);
    }


    public AccountResponse createAccount(AccountRequest accountRequest) {
        AccountEntity entity = new AccountEntity();
        entity.setCode(accountRequest.getCode());
        entity.setStatus(Status.ACTIVO);
        entity.setDescription(accountRequest.getDescription());
        //  set parent id
        AccountEntity parent = iAccountRepository.findById(accountRequest.getParentId())
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ParentID"));
        entity.setParent(parent);
        entity.setTypicalBalance(accountRequest.getTypicalBalance());
        Long categoryId = accountRequest.getCategory().longValue();
        AccountCategoryEntity accountCategoryEntity = categoryRepository.findById(categoryId)
                                                        .orElseThrow(()-> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Category"));
        entity.setAccountCategory(accountCategoryEntity);
        entity.setSupportsRegistration(accountRequest.isSupportsRegistration());
        iAccountRepository.save(entity);

        return  toResponse(entity);
    }

    public AccountResponse updateAccount(Long id, AccountRequest accountRequest) {
        logger.info("Updating account with ID: {}", id);
        //account exist
        AccountEntity existingAccount = iAccountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("No account found with ID: %d", id)));
        //check if code exist
        if(iAccountRepository.existsByCodeAndNotId(accountRequest.getCode(), id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,String.format("Account with code %s already exists.",accountRequest.getCode()));
        }
        AccountEntity parent = iAccountRepository.findById(accountRequest.getParentId())
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ParentID"));
        existingAccount.setParent(parent);

        existingAccount.setCode(accountRequest.getCode());
        existingAccount.setDescription(accountRequest.getDescription());
        existingAccount.setTypicalBalance(accountRequest.getTypicalBalance());
        existingAccount.setSupportsRegistration(accountRequest.isSupportsRegistration());
        existingAccount.setStatus(accountRequest.getStatus());

        if(!accountRequest.getBalances().isEmpty() && accountRequest.isSupportsRegistration()) {
            // validate balances
            validateBalances(accountRequest.getBalances());
            // update balances
            existingAccount.getBalances().clear();
            existingAccount.getBalances().addAll(accountRequest.getBalances().stream().map(this::toBalancesEntity).toList());
        }

        iAccountRepository.save(existingAccount);
        return toResponse(existingAccount);
    }


    /*Return All Categories of Accounts*/
    public List<AccountCategory> getAllCategories() {
       return categoryRepository.findAll().stream().map(x->{
            AccountCategory dto = new AccountCategory();
            dto.setId(x.getId());
            dto.setName(x.getName());
            return dto;
        }).toList();
    }

    private void validateBalances(Set<AccountBalance> balances){

        long count = balances.stream().filter(AccountBalance::getIsCurrent).count();
        if (count == 0){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one balance must be current");
        }

        if(count > 1){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only one balance must be current");
        }

    }

    private BalancesEntity toBalancesEntity(AccountBalance balance) {
        BalancesEntity entity = new BalancesEntity();
        entity.setInitialBalance(balance.getInitialBalance());
        entity.setIsActual(balance.getIsCurrent());
        return entity;
    }

    private AccountResponse toResponse(AccountEntity entity) {
        AccountResponse response = new AccountResponse();
        response.setId(entity.getId());
        response.setName(entity.getDescription());
        response.setAccountCode(entity.getCode());
        response.setCategoryName(entity.getAccountCategory().getName());
        response.setCategoryId(entity.getAccountCategory().getId());
        // recursive query if parent is not null is a root account
        if(entity.getParent() !=null){
            response.setParentName(entity.getParent().getDescription());
            response.setParentId(entity.getParent().getId());
            response.setParentCode(entity.getParent().getCode());
        }
        String type = entity.getTypicalBalance().equalsIgnoreCase("C") ? "Credito" : "Debito";
        String status = entity.getStatus().equals(Status.ACTIVO) ? "Activa": "Inactiva";
        response.setTypicallyBalance(type);
        response.setStatus(status);
        response.setSupportEntry(entity.isSupportsRegistration());
        return response;
    }
}
