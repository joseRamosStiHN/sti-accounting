package com.sti.accounting.services;

import com.sti.accounting.entities.AccountCategoryEntity;
import com.sti.accounting.entities.AccountEntity;
import com.sti.accounting.entities.AccountTypeEntity;
import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.models.AccountCategory;
import com.sti.accounting.models.AccountRequest;
import com.sti.accounting.models.AccountResponse;
import com.sti.accounting.models.AccountType;
import com.sti.accounting.repositories.IAccountCategoryRepository;
import com.sti.accounting.repositories.IAccountRepository;
import com.sti.accounting.repositories.IAccountTypeRepository;
import com.sti.accounting.repositories.ITransactionRepository;
import com.sti.accounting.utils.Status;
import com.sti.accounting.utils.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    @Mock
    private IAccountRepository iAccountRepository;

    @Mock
    private ITransactionRepository transactionRepository;

    @Mock
    private IAccountCategoryRepository categoryRepository;

    @Mock
    private IAccountTypeRepository accountTypeRepository;

    @InjectMocks
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllAccount() {
        // Arrange
        String tenantId = "07d017c0-f33a-4133-9dd5-4c3def76da5d";
        TenantContext.setCurrentTenant(tenantId);

        AccountCategoryEntity category = new AccountCategoryEntity();
        category.setId(1L);
        category.setName("Balance General");

        AccountEntity account = new AccountEntity();
        account.setId(1L);
        account.setCode("1");
        account.setDescription("ACTIVO");
        account.setTenantId(tenantId);
        account.setAccountCategory(category);
        account.setTypicalBalance("C");
        account.setStatus(Status.ACTIVO);

        when(iAccountRepository.findAll()).thenReturn(Collections.singletonList(account));
        when(transactionRepository.existsByAccountIdAndTenantId(account.getId(), tenantId)).thenReturn(false);

        // Act
        var result = accountService.getAllAccount();

        // Assert
        assertEquals(1, result.size());
        assertEquals("ACTIVO", result.get(0).getName());
        assertEquals("Balance General", result.get(0).getCategoryName());
        assertEquals("Credito", result.get(0).getTypicallyBalance());
        assertEquals("Activa", result.get(0).getStatus());
    }

    @Test
    void getById() {
        // Arrange
        String tenantId = "07d017c0-f33a-4133-9dd5-4c3def76da5d";
        TenantContext.setCurrentTenant(tenantId);

        AccountCategoryEntity category = new AccountCategoryEntity();
        category.setId(1L);
        category.setName("Balance General");

        AccountEntity account = new AccountEntity();
        account.setId(1L);
        account.setCode("1");
        account.setDescription("PASIVO");
        account.setTenantId(tenantId);
        account.setAccountCategory(category);
        account.setTypicalBalance("C");
        account.setStatus(Status.ACTIVO);

        when(iAccountRepository.findById(1L)).thenReturn(Optional.of(account));

        // Act
        AccountResponse result = accountService.getById(1L);

        // Assert
        assertEquals("PASIVO", result.getName());
    }

    @Test
    void createAccount() {

        String tenantId = "b5cfa106-8dc3-4d8d-92e4-caaf596f74777";
        TenantContext.setCurrentTenant(tenantId);

        // Simula el comportamiento del repositorio de tipos de cuenta
        AccountTypeEntity accountType = new AccountTypeEntity();
        accountType.setId(1L);
        accountType.setName("Test Account Type");

        // Simula el comportamiento del repositorio de categorías
        AccountCategoryEntity category = new AccountCategoryEntity();
        category.setId(1L);
        category.setName("Test Category");

        // Arrange
        AccountRequest request = new AccountRequest();
        request.setCode("1");
        request.setDescription("ACTIVO");
        request.setTypicalBalance("C");
        request.setAccountType(BigDecimal.valueOf(1));
        request.setCategory(BigDecimal.valueOf(1));
        request.setSupportsRegistration(true);


        when(iAccountRepository.existsByCodeAndTenantId("1", tenantId)).thenReturn(false);
        when(accountTypeRepository.findById(1L)).thenReturn(Optional.of(accountType));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(iAccountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AccountResponse result = accountService.createAccount(request);

        // Assert
        assertEquals("ACTIVO", result.getName());
        verify(iAccountRepository).save(any(AccountEntity.class));
    }

    @Test
    void updateAccount() {
        // Arrange
        String tenantId = "tenant1";
        TenantContext.setCurrentTenant(tenantId);

        AccountRequest request = new AccountRequest();
        request.setCode("ACC001");
        request.setDescription("Updated Account");
        request.setTypicalBalance("D");
        request.setCategory(BigDecimal.valueOf(1));
        request.setAccountType(BigDecimal.valueOf(1));
        request.setSupportsRegistration(true);

        // Simula el comportamiento del repositorio de tipos de cuenta
        AccountTypeEntity accountType = new AccountTypeEntity();
        accountType.setId(1L);
        accountType.setName("Test Account Type");

        // Simula el comportamiento del repositorio de categorías
        AccountCategoryEntity category = new AccountCategoryEntity();
        category.setId(1L);
        category.setName("Test Category");

        // Crea una cuenta existente
        AccountEntity existingAccount = new AccountEntity();
        existingAccount.setId(1L);
        existingAccount.setCode("ACC001");
        existingAccount.setDescription("Old Account");
        existingAccount.setTenantId(tenantId);
        existingAccount.setAccountType(accountType);
        existingAccount.setAccountCategory(category);
        existingAccount.setStatus(Status.ACTIVO);

        // Crea balances para la cuenta existente
        BalancesEntity balance1 = new BalancesEntity();
        balance1.setInitialBalance(BigDecimal.valueOf(100));
        balance1.setTypicalBalance("D");
        balance1.setIsCurrent(true);

        existingAccount.setBalances(List.of(balance1));

        // Simula el comportamiento del repositorio
        when(iAccountRepository.findById(1L)).thenReturn(Optional.of(existingAccount));
        when(iAccountRepository.existsByCodeAndNotId("ACC001", 1L, tenantId)).thenReturn(false);
        when(accountTypeRepository.findById(1L)).thenReturn(Optional.of(accountType)); // Simula el tipo de cuenta
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category)); // Simula la categoría
        when(iAccountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AccountResponse result = accountService.updateAccount(1L, request);

        // Assert
        assertEquals("Updated Account", result.getName());
        assertEquals("ACC001", result.getAccountCode());
        verify(iAccountRepository).save(existingAccount);
    }

    @Test
    void getAllCategories() {
        // Arrange
        AccountCategoryEntity category = new AccountCategoryEntity();
        category.setId(1L);
        category.setName("Test Category");

        // Simula el comportamiento del repositorio de categorías
        when(categoryRepository.findAll()).thenReturn(Collections.singletonList(category));

        // Act
        List<AccountCategory> result = accountService.getAllCategories();

        // Assert
        assertEquals(1, result.size());
        assertEquals("Test Category", result.get(0).getName());
    }

    @Test
    void getAllAccountType() {
        // Arrange
        AccountTypeEntity accountType = new AccountTypeEntity();
        accountType.setId(1L);
        accountType.setName("Ingresos");

        when(accountTypeRepository.findAll()).thenReturn(Collections.singletonList(accountType));

        // Act
        List<AccountType> result = accountService.getAllAccountType();

        // Assert
        assertEquals(1, result.size());
        assertEquals("Ingresos", result.get(0).getName());
    }

    @Test
    void cloneCatalog() {
        // Arrange
        String sourceTenantId = "07d017c0-f33a-4133-9dd5-4c3def76da5d";
        String targetTenantId = "07d017c0-f33a-4133-9dd5-4c3def76da5L";
        TenantContext.setCurrentTenant(targetTenantId);

        AccountEntity sourceAccount = new AccountEntity();
        sourceAccount.setCode("1-1");
        sourceAccount.setDescription("ACTIVO CORRIENTE");
        sourceAccount.setStatus(Status.ACTIVO);
        sourceAccount.setTypicalBalance("C");
        sourceAccount.setSupportsRegistration(true);
        sourceAccount.setTenantId(sourceTenantId);

        when(iAccountRepository.findAllByTenantId(sourceTenantId)).thenReturn(Collections.singletonList(sourceAccount));
        when(iAccountRepository.findAllByTenantId(targetTenantId)).thenReturn(Collections.emptyList());

        // Act
        accountService.cloneCatalog(sourceTenantId);

        // Assert
        verify(iAccountRepository).save(any(AccountEntity.class));
    }
}