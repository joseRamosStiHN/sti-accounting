package com.sti.accounting.services;

import com.sti.accounting.entities.*;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.*;
import com.sti.accounting.utils.Currency;
import com.sti.accounting.utils.Motion;
import com.sti.accounting.utils.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    @InjectMocks
    private TransactionService transactionService;

    @Mock
    private ITransactionRepository transactionRepository;

    @Mock
    private IAccountRepository accountRepository;

    @Mock
    private IDocumentRepository documentRepository;

    @Mock
    private IAccountingJournalRepository accountingJournalRepository;

    @Mock
    private ControlAccountBalancesService controlAccountBalancesService;

    @Mock
    private AccountingPeriodService accountingPeriodService;

    @Mock
    private IAccountRepository iAccountRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TenantContext.setCurrentTenant("b5cfa106-8dc3-4d8d-92e4-caaf596f74777");
    }

    @Test
    void getAllTransaction() {
        // Arrange
        TransactionEntity transaction = createTransactionEntity(1L, "b5cfa106-8dc3-4d8d-92e4-caaf596f74777");

        // Configurar el mock para devolver la transacción
        when(transactionRepository.findAll()).thenReturn(List.of(transaction));

        // Act
        List<TransactionResponse> result = transactionService.getAllTransaction();

        // Assert
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getDocumentType());
        assertEquals("L", result.get(0).getCurrency());
    }

    @Test
    void getTransactionAccountsByActivePeriod() {
        // Arrange
        AccountingPeriodEntity activePeriod = new AccountingPeriodEntity();
        activePeriod.setId(1L);
        when(accountingPeriodService.getActivePeriod()).thenReturn(activePeriod);
        when(transactionRepository.getAccountTransactionSummary()).thenReturn(new ArrayList<>());

        // Act
        Map<String, List<AccountTransactionDTO>> result = transactionService.getTransactionAccountsByActivePeriod();

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void getByDocumentType() {
        // Arrange
        Long documentId = 1L;
        AccountingPeriodEntity activePeriod = new AccountingPeriodEntity();
        activePeriod.setId(1L);

        // Simula el periodo contable activo
        when(accountingPeriodService.getActivePeriod()).thenReturn(activePeriod);

        // Crea y configura la transacción
        TransactionEntity transaction = createTransactionEntity(1L, "b5cfa106-8dc3-4d8d-92e4-caaf596f74777");

        // Simula la búsqueda de transacciones por ID de documento y tenant
        when(transactionRepository.findByDocumentIdAndTenantId(documentId, "b5cfa106-8dc3-4d8d-92e4-caaf596f74777"))
                .thenReturn(List.of(transaction));

        // Act
        List<TransactionResponse> result = transactionService.getByDocumentType(documentId);

        // Assert
        assertEquals(1, result.size());
        assertEquals(documentId, result.get(0).getDocumentType());
    }

    @Test
    void getTransactionByDateRange() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now();

        TransactionEntity transaction = createTransactionEntity(1L, "b5cfa106-8dc3-4d8d-92e4-caaf596f74777");

        when(transactionRepository.findByCreateAtDateBetweenAndTenantId(startDate, endDate, "b5cfa106-8dc3-4d8d-92e4-caaf596f74777")).thenReturn(List.of(transaction));

        // Act
        List<TransactionResponse> result = transactionService.getTransactionByDateRange(startDate, endDate);

        // Assert
        assertEquals(1, result.size());
    }

    @Test
    void getById() {
        // Arrange
        Long transactionId = 1L;
        TransactionEntity transaction = createTransactionEntity(transactionId, "b5cfa106-8dc3-4d8d-92e4-caaf596f74777");
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(transaction));

        // Act
        TransactionResponse result = transactionService.getById(transactionId);

        // Assert
        assertNotNull(result);
    }

    @Test
    void createTransaction() {
        // Crea cuentas simuladas
        AccountEntity account1 = new AccountEntity();
        account1.setId(93L);
        account1.setCode("Account1");
        account1.setDescription("Test Account 1");

        AccountEntity account2 = new AccountEntity();
        account2.setId(94L);
        account2.setCode("Account2");
        account2.setDescription("Test Account 2");

        // Configura el mock para devolver las cuentas
        when(iAccountRepository.findAll()).thenReturn(Arrays.asList(account1, account2));

        // Arrange
        TransactionRequest request = new TransactionRequest();
        request.setDocumentType(1L);
        request.setDiaryType(1L);
        request.setCreateAtDate(LocalDate.now());
        request.setReference("Test Reference");
        request.setExchangeRate(BigDecimal.ONE);
        request.setDescriptionPda("Test Description");

        // Crea detalles de transacción balanceados
        TransactionDetailRequest creditDetail = new TransactionDetailRequest();
        creditDetail.setAccountId(93L);
        creditDetail.setAmount(BigDecimal.valueOf(100));
        creditDetail.setMotion(Motion.C);

        TransactionDetailRequest debitDetail = new TransactionDetailRequest();
        debitDetail.setAccountId(94L);
        debitDetail.setAmount(BigDecimal.valueOf(100));
        debitDetail.setMotion(Motion.D);

        // Agrega los detalles a la lista
        request.setDetail(Arrays.asList(creditDetail, debitDetail));

        DocumentEntity document = new DocumentEntity();
        document.setId(1L);

        AccountingJournalEntity journal = new AccountingJournalEntity();
        journal.setId(1L);
        journal.setTenantId("b5cfa106-8dc3-4d8d-92e4-caaf596f74777");

        when(documentRepository.findById(request.getDocumentType())).thenReturn(Optional.of(document));
        when(accountingJournalRepository.findById(request.getDiaryType())).thenReturn(Optional.of(journal));
        when(accountingPeriodService.getActivePeriod()).thenReturn(new AccountingPeriodEntity());

        // Act
        TransactionResponse result = transactionService.createTransaction(request);

        // Assert
        assertNotNull(result);
        verify(transactionRepository, times(1)).save(any(TransactionEntity.class));
    }

    @Test
    void updateTransaction() {
        // Arrange
        Long transactionId = 1L;
        TransactionRequest request = new TransactionRequest();
        request.setDocumentType(1L);
        request.setDiaryType(1L);
        request.setCreateAtDate(LocalDate.now());
        request.setReference("Updated Reference");
        request.setExchangeRate(BigDecimal.ONE);
        request.setDescriptionPda("Updated Description");
        request.setDetail(new ArrayList<>());

        TransactionEntity existingTransaction = new TransactionEntity();
        existingTransaction.setTenantId("test-tenant");
        when(transactionRepository.findById(transactionId)).thenReturn(Optional.of(existingTransaction));
        when(documentRepository.findById(request.getDocumentType())).thenReturn(Optional.of(new DocumentEntity()));
        when(accountingJournalRepository.findById(request.getDiaryType())).thenReturn(Optional.of(new AccountingJournalEntity()));
        when(accountingPeriodService.getActivePeriod()).thenReturn(new AccountingPeriodEntity());

        // Act
        TransactionResponse result = transactionService.updateTransaction(transactionId, request);

        // Assert
        assertNotNull(result);
        verify(transactionRepository, times(1)).save(existingTransaction);
    }

    @Test
    void changeTransactionStatus() {
        // Arrange
        List<Long> transactionIds = List.of(1L, 2L);
        TransactionEntity transaction1 = new TransactionEntity();
        transaction1.setStatus(StatusTransaction.DRAFT);
        TransactionEntity transaction2 = new TransactionEntity();
        transaction2.setStatus(StatusTransaction.SUCCESS);
        when(transactionRepository.findAllById(transactionIds)).thenReturn(List.of(transaction1, transaction2));

        // Act
        transactionService.changeTransactionStatus(transactionIds);

        // Assert
        assertEquals(StatusTransaction.SUCCESS, transaction1.getStatus());
        verify(transactionRepository, times(1)).save(transaction1);
        verify(controlAccountBalancesService, times(1)).updateControlAccountBalances(transaction1);
    }

    private TransactionEntity createTransactionEntity(Long id, String tenantId) {
        DocumentEntity document = new DocumentEntity();
        document.setId(1L);
        document.setName("Test Document");

        AccountingJournalEntity accountingJournal = new AccountingJournalEntity();
        accountingJournal.setId(1L);
        accountingJournal.setDiaryName("Ingresos");

        AccountingPeriodEntity activePeriod = new AccountingPeriodEntity();
        activePeriod.setId(1L);

        TransactionEntity transaction = new TransactionEntity();
        transaction.setId(id);
        transaction.setCreateAtDate(LocalDate.now());
        transaction.setStatus(StatusTransaction.DRAFT);
        transaction.setReference("Test Reference");
        transaction.setDocument(document);
        transaction.setExchangeRate(BigDecimal.valueOf(26.5));
        transaction.setDescriptionPda("Test Description");
        transaction.setCurrency(Currency.L);
        transaction.setTypeSale("CONTADO");
        transaction.setCashValue(BigDecimal.valueOf(50));
        transaction.setCreditValue(BigDecimal.valueOf(100));
        transaction.setTypePayment("EFECTIVO");
        transaction.setRtn("NO");
        transaction.setSupplierName("Test Supplier");
        transaction.setAccountingJournal(accountingJournal);
        transaction.setTenantId(tenantId);
        transaction.setStatus(StatusTransaction.DRAFT);
        transaction.setAccountingPeriod(activePeriod);
        transaction.setTransactionDetail(new ArrayList<>());

        return transaction;
    }
}