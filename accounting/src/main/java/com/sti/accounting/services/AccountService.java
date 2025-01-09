package com.sti.accounting.services;

import com.sti.accounting.entities.AccountCategoryEntity;
import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.entities.AccountTypeEntity;
import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.IAccountCategoryRepository;
import com.sti.accounting.repositories.IAccountRepository;
import com.sti.accounting.repositories.IAccountTypeRepository;
import com.sti.accounting.repositories.ITransactionRepository;
import com.sti.accounting.utils.Status;
import com.sti.accounting.utils.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private final IAccountRepository iAccountRepository;
    private final IAccountCategoryRepository categoryRepository;
    private final IAccountTypeRepository accountTypeRepository;
    private final ITransactionRepository transactionRepository;

    public AccountService(IAccountRepository iAccountRepository, IAccountCategoryRepository categoryRepository, IAccountTypeRepository accountTypeRepository, ITransactionRepository transactionRepository) {
        this.iAccountRepository = iAccountRepository;
        this.categoryRepository = categoryRepository;
        this.accountTypeRepository = accountTypeRepository;
        this.transactionRepository = transactionRepository;
    }

    private String getTenantId() {
        return TenantContext.getCurrentTenant();
    }

    public List<AccountResponse> getAllAccount() {
        String tenantId = getTenantId();
        return this.iAccountRepository.findAll().stream().filter(account -> account.getTenantId().equals(tenantId)).map(this::toResponse).toList();
    }

    public AccountResponse getById(Long id) {
        logger.trace("account request with id {}", id);
        String tenantId = getTenantId();
        AccountEntity accountEntity = iAccountRepository.findById(id).filter(account -> account.getTenantId().equals(tenantId)).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("No account were found with the id %s", id))
        );
        return toResponse(accountEntity);
    }


    public AccountResponse createAccount(AccountRequest accountRequest) {
        AccountEntity entity = new AccountEntity();
        String tenantId = getTenantId();

        boolean existsCode = this.iAccountRepository.existsByCodeAndTenantId(accountRequest.getCode(), tenantId);
        if (existsCode) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The account code already exists.");
        }
        entity.setCode(accountRequest.getCode());
        entity.setStatus(Status.ACTIVO);
        entity.setDescription(accountRequest.getDescription());
        //  set parent id
        AccountEntity parent = null;
        if (accountRequest.getParentId() != null) {
            parent = iAccountRepository.findById(accountRequest.getParentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ParentID"));
        }
        entity.setParent(parent);
        entity.setTypicalBalance(accountRequest.getTypicalBalance());

        if (accountRequest.getAccountType() != null) {
            Long accountTypeId = accountRequest.getAccountType().longValue();
            AccountTypeEntity accountTypeEntity = accountTypeRepository.findById(accountTypeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Account Type"));
            entity.setAccountType(accountTypeEntity);
        }

        Long categoryId = accountRequest.getCategory().longValue();
        AccountCategoryEntity accountCategoryEntity = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Category"));
        entity.setAccountCategory(accountCategoryEntity);
        entity.setSupportsRegistration(accountRequest.isSupportsRegistration());
        entity.setTenantId(tenantId);

        if (!accountRequest.getBalances().isEmpty() && accountRequest.isSupportsRegistration()) {
            validateBalances(accountRequest.getBalances());
            List<BalancesEntity> balancesList = accountRequest.getBalances().stream()
                    .map(balance -> {
                        BalancesEntity balancesEntity = toBalancesEntity(balance);
                        balancesEntity.setAccount(entity);
                        return balancesEntity;
                    })
                    .toList();

            entity.setBalances(balancesList);
        }


        iAccountRepository.save(entity);

        return toResponse(entity);
    }

    public AccountResponse updateAccount(Long id, AccountRequest accountRequest) {
        logger.info("Updating account with ID: {}", id);
        String tenantId = getTenantId();

        // Verificar si la cuenta existe
        AccountEntity existingAccount = iAccountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("No account found with ID: %d", id)));

        // Verificar si el código de cuenta ya existe para otra cuenta
        if (iAccountRepository.existsByCodeAndNotId(accountRequest.getCode(), id, tenantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("Account with code %s already exists.", accountRequest.getCode()));
        }

        // Establecer el padre de la cuenta si se proporciona
        AccountEntity parent = null;
        if (accountRequest.getParentId() != null) {
            parent = iAccountRepository.findById(accountRequest.getParentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ParentID"));
        }
        existingAccount.setParent(parent);

        // Actualizar los atributos de la cuenta
        existingAccount.setCode(accountRequest.getCode());
        existingAccount.setDescription(accountRequest.getDescription());
        existingAccount.setTypicalBalance(accountRequest.getTypicalBalance());
        existingAccount.setSupportsRegistration(accountRequest.isSupportsRegistration());
        existingAccount.setStatus(accountRequest.getStatus());
        existingAccount.setTenantId(tenantId);

        if (accountRequest.getAccountType() != null) {
            Long accountTypeId = accountRequest.getAccountType().longValue();
            AccountTypeEntity accountTypeEntity = accountTypeRepository.findById(accountTypeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Account Type"));
            existingAccount.setAccountType(accountTypeEntity);
        }

        // Si se soporta el registro, actualizar los balances
        if (accountRequest.isSupportsRegistration()) {
            validateBalances(accountRequest.getBalances());

            existingAccount.getBalances().clear();

            List<BalancesEntity> balancesList = accountRequest.getBalances().stream()
                    .map(balance -> {
                        BalancesEntity balancesEntity = toBalancesEntity(balance);
                        balancesEntity.setAccount(existingAccount);
                        return balancesEntity;
                    })
                    .toList();

            existingAccount.getBalances().addAll(balancesList);
        }

        iAccountRepository.save(existingAccount);

        return toResponse(existingAccount);
    }


    /*Return All Categories of Accounts*/
    public List<AccountCategory> getAllCategories() {

        return categoryRepository.findAll().stream().map(x -> {
            AccountCategory dto = new AccountCategory();
            dto.setId(x.getId());
            dto.setName(x.getName());
            return dto;
        }).toList();
    }

    /*Return All Account Type of Accounts*/
    public List<AccountType> getAllAccountType() {
        return accountTypeRepository.findAll().stream().map(x -> {
            AccountType dto = new AccountType();
            dto.setId(x.getId());
            dto.setName(x.getName());
            return dto;
        }).toList();
    }

    private void validateBalances(Set<AccountBalance> balances) {

        long count = balances.stream().filter(AccountBalance::getIsCurrent).count();
        if (count == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one balance must be current");
        }

        if (count > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only one balance must be current");
        }

        for (AccountBalance balance : balances) {
            if (balance.getInitialBalance().compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The initial account balance cannot be negative");
            }
        }

    }

    private BalancesEntity toBalancesEntity(AccountBalance balance) {
        BalancesEntity entity = new BalancesEntity();
        entity.setInitialBalance(balance.getInitialBalance());
        entity.setTypicalBalance(balance.getTypicalBalance());
        entity.setIsCurrent(balance.getIsCurrent());
        return entity;
    }

    private AccountResponse toResponse(AccountEntity entity) {
        String tenantId = getTenantId();

        AccountResponse response = new AccountResponse();
        response.setId(entity.getId());
        response.setName(entity.getDescription());
        response.setAccountCode(entity.getCode());
        response.setCategoryName(entity.getAccountCategory().getName());
        response.setCategoryId(entity.getAccountCategory().getId());

        // Verifica si la cuenta tiene transacciones
        boolean hasTransactions = transactionRepository.existsByAccountIdAndTenantId(entity.getId(), tenantId);
        response.setAsTransaction(hasTransactions);

        // Verifica si la cuenta tiene cuentas hijas
        boolean hasChildAccounts = iAccountRepository.countByParentIdAndTenantId(entity.getId(), tenantId) > 0;
        response.setHasChildAccounts(hasChildAccounts);

        // recursive query if parent is not null is a root account
        if (entity.getParent() != null) {
            response.setParentName(entity.getParent().getDescription());
            response.setParentId(entity.getParent().getId());
            response.setParentCode(entity.getParent().getCode());
        }
        String type = entity.getTypicalBalance().equalsIgnoreCase("C") ? "Credito" : "Debito";
        String status = entity.getStatus().equals(Status.ACTIVO) ? "Activa" : "Inactiva";
        response.setTypicallyBalance(type);
        if (entity.getAccountType() != null) {
            response.setAccountTypeName(entity.getAccountType().getName());
            response.setAccountType(entity.getAccountType().getId());
        }

        response.setStatus(status);
        response.setSupportEntry(entity.isSupportsRegistration());
        // Convertir balances de List<BalancesEntity> a Set<AccountBalance>
        Set<AccountBalance> balanceSet = new HashSet<>();
        List<BalancesEntity> balances = entity.getBalances();
        if (balances != null) {
            for (BalancesEntity balanceEntity : balances) {
                AccountBalance accountBalance = convertToAccountBalance(balanceEntity);
                balanceSet.add(accountBalance);
            }
        }
        response.setBalances(balanceSet);
        return response;
    }

    private AccountBalance convertToAccountBalance(BalancesEntity balanceEntity) {
        AccountBalance accountBalance = new AccountBalance();
        accountBalance.setId(balanceEntity.getId());
        accountBalance.setTypicalBalance(balanceEntity.getTypicalBalance());
        accountBalance.setInitialBalance(balanceEntity.getInitialBalance());
        accountBalance.setCreateAtDate(balanceEntity.getCreateAtDate());
        accountBalance.setIsCurrent(balanceEntity.getIsCurrent());
        return accountBalance;
    }

}
