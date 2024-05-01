package com.sti.accounting.services;

import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.models.AccountRequest;
import com.sti.accounting.repositories.IAccountRepository;
import jakarta.ws.rs.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;

import com.sti.accounting.utils.AppUtil;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private final IAccountRepository iAccountRepository;

    public AccountService(IAccountRepository iAccountRepository) {
        this.iAccountRepository = iAccountRepository;
    }

    public List<AccountRequest> GetAllAccount() {
        List<AccountEntity> entities = this.iAccountRepository.findAll();
        return entities.stream().map(AccountEntity::entityToRequest).collect(Collectors.toList());
    }

    public AccountRequest GetById(Long id) {
        logger.trace("account request with id {}", id);
        AccountEntity accountEntity = iAccountRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST,String.format("No account were found with the id %s", id))
        );
        return accountEntity.entityToRequest();
    }

    //TODO: ESTO ESTA MAL NO SE DEBE RETORNAR EL ENTITY
    public AccountEntity CreateAccount(AccountRequest accountRequest) {
        logger.info("creating account");
        try {
            if (iAccountRepository.existsByCode(accountRequest.getCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Account with code " + accountRequest.getCode() + " already exists.");
            }

            AccountEntity newAccount = new AccountEntity();
            newAccount.setCode(accountRequest.getCode());
            newAccount.setDescription(accountRequest.getDescription());
            newAccount.setParentId(accountRequest.getParentId());
            newAccount.setCategory(accountRequest.getCategory());
            newAccount.setTypicalBalance(accountRequest.getTypicalBalance());
            newAccount.setSupportsRegistration(accountRequest.isSupportsRegistration());
            newAccount.setStatus(accountRequest.getStatus());

            if (!accountRequest.isSupportsRegistration()) {
                newAccount.setBalances(AppUtil.buildBalance(newAccount));
            } else {
                if (accountRequest.getBalances().size() != 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only one balance allowed per account.");
                }

                List<BalancesEntity> balances = accountRequest.getBalances().stream().map(balanceRequest -> {
                    BalancesEntity balancesEntity = new BalancesEntity();
                    balancesEntity.setInitialBalance(balanceRequest.getInitialBalance());
                    balancesEntity.setIsActual(balanceRequest.getIsActual());
                    balancesEntity.setAccount(newAccount);
                    return balancesEntity;
                }).collect(Collectors.toList());

                newAccount.setBalances(balances);
            }

            return iAccountRepository.save(newAccount);
        } catch (Exception e) {
            throw new RuntimeException("Error creating account: " + e.getMessage());
        }
    }

    public AccountEntity UpdateAccount(Long id, AccountRequest accountRequest) {
        logger.info("Updating account with ID: {}", id);

        AccountEntity existingAccount = iAccountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("No account found with ID: %d", id)));

        if (!existingAccount.getCode().equals(accountRequest.getCode()) && iAccountRepository.existsByCode(accountRequest.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Account with code " + accountRequest.getCode() + " already exists.");
        }

        existingAccount.setCode(accountRequest.getCode());
        existingAccount.setDescription(accountRequest.getDescription());
        existingAccount.setParentId(accountRequest.getParentId());
        existingAccount.setCategory(accountRequest.getCategory());
        existingAccount.setTypicalBalance(accountRequest.getTypicalBalance());
        existingAccount.setSupportsRegistration(accountRequest.isSupportsRegistration());
        existingAccount.setStatus(accountRequest.getStatus());

        if (!accountRequest.isSupportsRegistration()) {
            existingAccount.getBalances().clear();
        } else {
            if (accountRequest.getBalances().size() != 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Only one balance allowed per account.");
            }

            List<BalancesEntity> balances = accountRequest.getBalances().stream().map(balanceRequest -> {
                if (balanceRequest.getId() != null) {
                    BalancesEntity existingBalancesEntity = existingAccount.getBalances().stream()
                            .filter(b -> b.getId().equals(balanceRequest.getId()))
                            .findFirst()
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    String.format("No balance entity found with ID: %d", balanceRequest.getId())));

                    existingBalancesEntity.setInitialBalance(balanceRequest.getInitialBalance());
                    existingBalancesEntity.setIsActual(balanceRequest.getIsActual());
                    return existingBalancesEntity;
                } else {
                    BalancesEntity newBalancesEntity = new BalancesEntity();
                    newBalancesEntity.setInitialBalance(balanceRequest.getInitialBalance());
                    newBalancesEntity.setCreateAtDate(LocalDateTime.now());
                    newBalancesEntity.setIsActual(balanceRequest.getIsActual());
                    newBalancesEntity.setAccount(existingAccount);
                    return newBalancesEntity;
                }
            }).toList();

            existingAccount.getBalances().clear();
            existingAccount.getBalances().addAll(balances);
        }

        return iAccountRepository.save(existingAccount);


    }


}
