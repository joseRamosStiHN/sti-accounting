package com.sti.accounting.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Service
public class AccountService {
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private final IAccountRepository iAccountRepository;
    private final IAccountCategoryRepository categoryRepository;
    private final IAccountTypeRepository accountTypeRepository;
    private final ITransactionRepository transactionRepository;
    private final AuthService authService;

    public AccountService(IAccountRepository iAccountRepository, IAccountCategoryRepository categoryRepository, IAccountTypeRepository accountTypeRepository, ITransactionRepository transactionRepository, AuthService authService) {
        this.iAccountRepository = iAccountRepository;
        this.categoryRepository = categoryRepository;
        this.accountTypeRepository = accountTypeRepository;
        this.transactionRepository = transactionRepository;
        this.authService = authService;
    }

    public List<AccountResponse> getAllAccount() {
        String tenantId = authService.getTenantId();
        return this.iAccountRepository.findAllByTenantId(tenantId).stream().map(this::toResponse).toList();
    }

    public AccountResponse getById(Long id) {
        logger.trace("account request with id {}", id);
        String tenantId = authService.getTenantId();
        AccountEntity accountEntity = iAccountRepository.findById(id).filter(account -> account.getTenantId().equals(tenantId)).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("No account were found with the id %s", id))
        );
        return toResponse(accountEntity);
    }


    public AccountResponse createAccount(AccountRequest accountRequest) {
        AccountEntity entity = new AccountEntity();
        String tenantId = authService.getTenantId();

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
        String tenantId = authService.getTenantId();

        // Verificar si la cuenta existe
        AccountEntity existingAccount = iAccountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        String.format("No account found with ID: %d", id)));

        // Guardar el código antiguo para comparación
        String oldCode = existingAccount.getCode();

        // Verificar si el código de cuenta ya existe para otra cuenta
        if (iAccountRepository.existsByCodeAndNotId(accountRequest.getCode(), id, tenantId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Account with code %s already exists.", accountRequest.getCode()));
        }

        // Validar integridad jerárquica
        validateHierarchyIntegrity(existingAccount, accountRequest, tenantId);

        // Establecer el padre de la cuenta si se proporciona
        AccountEntity parent = null;
        if (accountRequest.getParentId() != null) {
            parent = iAccountRepository.findById(accountRequest.getParentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ParentID"));

            // Validar que el nuevo código mantenga la jerarquía
            if (!accountRequest.getCode().startsWith(parent.getCode())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Child account code must start with parent account code");
            }
        }
        existingAccount.setParent(parent);

        // Actualizar los atributos de la cuenta
        existingAccount.setCode(accountRequest.getCode());
        existingAccount.setDescription(accountRequest.getDescription());
        existingAccount.setTypicalBalance(accountRequest.getTypicalBalance());
        existingAccount.setSupportsRegistration(accountRequest.isSupportsRegistration());
        existingAccount.setStatus(accountRequest.getStatus());
        existingAccount.setTenantId(tenantId);

        // Actualizar tipo de cuenta si se proporciona
        if (accountRequest.getAccountType() != null) {
            Long accountTypeId = accountRequest.getAccountType().longValue();
            AccountTypeEntity accountTypeEntity = accountTypeRepository.findById(accountTypeId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Account Type"));
            existingAccount.setAccountType(accountTypeEntity);
        }

        // Actualizar balances si soporta registros
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

        // Guardar la cuenta actualizada
        AccountEntity updatedAccount = iAccountRepository.save(existingAccount);

        // Si el código ha cambiado y esta cuenta tiene hijos, actualizar sus códigos
        if (!oldCode.equals(accountRequest.getCode()) &&
                iAccountRepository.countByParentIdAndTenantId(updatedAccount.getId(), tenantId) > 0) {
            updateChildAccountCodes(updatedAccount, oldCode, accountRequest.getCode());
        }

        return toResponse(updatedAccount);
    }

    private void validateHierarchyIntegrity(AccountEntity existingAccount, AccountRequest accountRequest, String tenantId) {
        // Validar que si es cuenta hija, no se pueda quitar la referencia al padre
        if (existingAccount.getParent() != null && accountRequest.getParentId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot remove parent reference from a child account");
        }

        // Validar que el nuevo padre no sea uno de sus hijos
        if (accountRequest.getParentId() != null) {
            if (isChildAccount(existingAccount, accountRequest.getParentId(), tenantId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot set a child account as parent (would create a circular reference)");
            }
        }
    }

    private boolean isChildAccount(AccountEntity account, Long potentialChildId, String tenantId) {
        List<AccountEntity> children = iAccountRepository.findByParentIdAndTenantId(account.getId(), tenantId);

        // Verificar si el ID potencial está entre los hijos directos
        if (children.stream().anyMatch(child -> child.getId().equals(potentialChildId))) {
            return true;
        }

        // Verificar recursivamente en los nietos
        for (AccountEntity child : children) {
            if (isChildAccount(child, potentialChildId, tenantId)) {
                return true;
            }
        }

        return false;
    }

    private void updateChildAccountCodes(AccountEntity parentAccount, String oldParentCode, String newParentCode) {
        String tenantId = authService.getTenantId();
        List<AccountEntity> childAccounts = iAccountRepository.findByParentIdAndTenantId(parentAccount.getId(), tenantId);

        for (AccountEntity child : childAccounts) {
            // Obtener la parte del código después del código del padre
            String childSuffix = child.getCode().substring(child.getCode().indexOf('-'));

            // Construir el nuevo código combinando el nuevo código del padre y el sufijo del hijo
            String newChildCode = newParentCode + childSuffix;
            child.setCode(newChildCode);
            iAccountRepository.save(child);

            // Si este hijo tiene sus propios hijos, actualizarlos también
            if (iAccountRepository.countByParentIdAndTenantId(child.getId(), tenantId) > 0) {
                updateChildAccountCodes(child, oldParentCode + childSuffix, newChildCode);
            }
        }
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

    public void cloneCatalog(String sourceTenantId) {
        String tenantId = authService.getTenantId();

        // Verificar si ya existen cuentas en el tenant actual
        List<AccountEntity> existingAccounts = iAccountRepository.findAllByTenantId(tenantId);
        if (!existingAccounts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The catalog cannot be cloned because accounts already exist for the tenant.");
        }


        // Si sourceTenantId es null o no se proporciona, obtener cuentas desde el archivo JSON
        if (sourceTenantId == null || sourceTenantId.isEmpty()) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                // Leer el archivo JSON
                InputStream inputStream = getClass().getClassLoader().getResourceAsStream("accounting_catalog.json");
                List<CloneAccountDTO> cloneAccountDTOs = objectMapper.readValue(
                        inputStream,
                        new TypeReference<List<CloneAccountDTO>>() {}
                );

                for(CloneAccountDTO cloneDto : cloneAccountDTOs) {
                    processData(cloneDto, null, tenantId);
                }
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error reading accounts from JSON file", e);
            }
        } else {
            // Mapear entidades a AccountRequest si vienen de la base de datos
            List<AccountRequest> sourceAccounts = iAccountRepository.findAllByTenantId(sourceTenantId).stream().map(account -> {
                AccountRequest request = new AccountRequest();
                request.setId(account.getId());
                request.setCode(account.getCode());
                request.setDescription(account.getDescription());
                request.setTypicalBalance(account.getTypicalBalance());
                request.setSupportsRegistration(account.isSupportsRegistration());
                request.setStatus(account.getStatus());
                request.setCategory(account.getAccountCategory() != null ? BigDecimal.valueOf(account.getAccountCategory().getId()) : null);
                request.setAccountType(account.getAccountType() != null ? BigDecimal.valueOf(account.getAccountType().getId()) : null);
                request.setParentId(account.getParent() != null ? account.getId() : null);
                return request;
            }).toList();

            for (AccountRequest sourceAccount : sourceAccounts) {
                AccountEntity clonedAccount = new AccountEntity();
                clonedAccount.setCode(sourceAccount.getCode());
                clonedAccount.setDescription(sourceAccount.getDescription());
                clonedAccount.setStatus(sourceAccount.getStatus());
                clonedAccount.setTypicalBalance(sourceAccount.getTypicalBalance());
                clonedAccount.setSupportsRegistration(sourceAccount.isSupportsRegistration());
                clonedAccount.setTenantId(tenantId);

                // Clonar el tipo de cuenta
                if (sourceAccount.getAccountType() != null) {
                    AccountTypeEntity accountTypeEntity = accountTypeRepository.findById(sourceAccount.getAccountType().longValue())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Account Type"));
                    clonedAccount.setAccountType(accountTypeEntity);
                }

                // Clonar la categoría de cuenta
                if (sourceAccount.getCategory() != null) {
                    AccountCategoryEntity accountCategoryEntity = categoryRepository.findById(sourceAccount.getCategory().longValue())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Category"));
                    clonedAccount.setAccountCategory(accountCategoryEntity);
                }

                if (sourceAccount.getParentId() != null) {
                    AccountEntity parentAccount = iAccountRepository.findById(sourceAccount.getParentId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Parent Account"));
                    clonedAccount.setParent(parentAccount);
                }

                iAccountRepository.save(clonedAccount);
            }
        }


    }

    private AccountResponse toResponse(AccountEntity entity) {
        String tenantId = authService.getTenantId();

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
    
    private void processData(CloneAccountDTO cloneAccountDTO, AccountEntity account, String tenantId) {
        AccountEntity accountEntity = new AccountEntity();
        accountEntity.setCode(cloneAccountDTO.getCode());
        accountEntity.setDescription(cloneAccountDTO.getDescription());
        accountEntity.setTypicalBalance(cloneAccountDTO.getTypicalBalance());
        accountEntity.setSupportsRegistration(cloneAccountDTO.isSupportsRegistration());
        accountEntity.setStatus(Status.valueOf(cloneAccountDTO.getStatus()));

        List<BalancesEntity> balance = new ArrayList<>();
        accountEntity.setBalances(balance);

        AccountCategoryEntity category = new AccountCategoryEntity();
        category.setId(cloneAccountDTO.getAccountCategory().getId());
        accountEntity.setAccountCategory(category);
        
        if (cloneAccountDTO.getParentId() != null) {
            accountEntity.setParent(account);
        }
        
        if (cloneAccountDTO.getAccountType() != null) {
            logger.info("ID_ACCOUNT_TYPE: {}", cloneAccountDTO.getAccountType().getId());
            AccountTypeEntity type = new AccountTypeEntity();
            type.setId(cloneAccountDTO.getAccountType().getId());
            accountEntity.setAccountType(type);
        }
        
        accountEntity.setTenantId(tenantId);

        AccountEntity saveAccount = iAccountRepository.save(accountEntity);

        if(cloneAccountDTO.getChildren() != null){
            for(CloneAccountDTO cloneAccountDTOChild : cloneAccountDTO.getChildren()){
                processData(cloneAccountDTOChild, saveAccount, tenantId);
            }
        }

    }
}
