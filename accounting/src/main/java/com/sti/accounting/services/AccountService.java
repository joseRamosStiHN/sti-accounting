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
import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private final IAccountRepository iAccountRepository;

    public AccountService(IAccountRepository iAccountRepository) {
        this.iAccountRepository = iAccountRepository;
    }

    public List<AccountRequest> getAllAccount() {
        List<AccountEntity> entities = this.iAccountRepository.findAll();
        return entities.stream().map(AccountEntity::entityToRequest).collect(Collectors.toList());
    }

    public AccountRequest getById(Long id) {
        logger.trace("account request with id {}", id);
        AccountEntity accountEntity = iAccountRepository.findById(id).orElseThrow(
                () -> new BadRequestException(String.format("No account were found with the id %s", id))
        );
        return accountEntity.entityToRequest();
    }

    public AccountEntity createAccount(AccountRequest accountRequest) {
        logger.info("creating account");
        try {
            AccountEntity newAccount = new AccountEntity();
            newAccount.setCode(accountRequest.getCode());
            newAccount.setDescription(accountRequest.getDescription());
            newAccount.setParentId(accountRequest.getParentId());
            newAccount.setCategory(accountRequest.getCategory());
            newAccount.setTypicalBalance(accountRequest.getTypicalBalance());
            newAccount.setSupportsRegistration(accountRequest.isSupportsRegistration());


            long actualBalancesCount = accountRequest.getBalances().stream()
                    .filter(BalancesEntity::getIsActual)
                    .count();
            if (actualBalancesCount > 1) {
                throw new BadRequestException("There can only be one current balance per account.");
            }

            List<BalancesEntity> balances = accountRequest.getBalances().parallelStream().map(x -> {

                BalancesEntity balancesEntity = new BalancesEntity();
                balancesEntity.setInitialBalance(x.getInitialBalance());
                balancesEntity.setIsActual(x.getIsActual());
                balancesEntity.setAccount(newAccount);
                return balancesEntity;
            }).toList();

            newAccount.setBalances(balances);

            return iAccountRepository.save(newAccount);
        } catch (Exception e) {
            logger.error("Error creating account: {}", e.getMessage());
            throw new RuntimeException("Error creating account: " + e.getMessage());
        }
    }

    public AccountEntity updateAccount(Long id, AccountRequest accountRequest) {
        logger.info("Updating account with ID: {}", id);
        try {
            AccountEntity existingAccount = iAccountRepository.findById(id)
                    .orElseThrow(() -> new BadRequestException(
                            String.format("No account found with ID: %d", id)));

            existingAccount.setCode(accountRequest.getCode());
            existingAccount.setDescription(accountRequest.getDescription());
            existingAccount.setParentId(accountRequest.getParentId());
            existingAccount.setCategory(accountRequest.getCategory());
            existingAccount.setTypicalBalance(accountRequest.getTypicalBalance());
            existingAccount.setSupportsRegistration(accountRequest.isSupportsRegistration());

            long actualBalancesCount = accountRequest.getBalances().stream()
                    .filter(BalancesEntity::getIsActual)
                    .count();
            if (actualBalancesCount > 1) {
                throw new BadRequestException("There can only be one current balance per account.");
            }

            List<BalancesEntity> balancesEntities = new ArrayList<>();
            if (accountRequest.getBalances() != null) {
                for (BalancesEntity balancesRequest : accountRequest.getBalances()) {
                    if (balancesRequest.getId() != null) {
                        BalancesEntity existingBalancesEntity = existingAccount.getBalances().stream()
                                .filter(b -> b.getId().equals(balancesRequest.getId()))
                                .findFirst()
                                .orElseThrow(() -> new BadRequestException(
                                        String.format("No balance entity found with ID: %d", balancesRequest.getId())));

                        existingBalancesEntity.setInitialBalance(balancesRequest.getInitialBalance());
                        existingBalancesEntity.setIsActual(balancesRequest.getIsActual());
                        balancesEntities.add(existingBalancesEntity);
                    } else {
                        BalancesEntity newBalancesEntity = new BalancesEntity();
                        newBalancesEntity.setInitialBalance(balancesRequest.getInitialBalance());
                        newBalancesEntity.setCreateAtDate(LocalDateTime.now());
                        newBalancesEntity.setIsActual(balancesRequest.getIsActual());
                        newBalancesEntity.setAccount(existingAccount);

                        balancesEntities.add(newBalancesEntity);
                    }
                }
            }
            existingAccount.getBalances().clear();
            existingAccount.getBalances().addAll(balancesEntities);

            return iAccountRepository.save(existingAccount);

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error updating account: {}", e.getMessage());
            throw new RuntimeException("Error updating account: " + e.getMessage());
        }
    }

    public void deleteAccount(Long id) {
        logger.info("delete account");
        AccountEntity existingAccount = iAccountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("No account found with ID: %d", id)));

        iAccountRepository.deleteById(id);
    }


}
