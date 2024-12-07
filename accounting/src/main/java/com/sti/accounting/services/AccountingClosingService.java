package com.sti.accounting.services;

import com.sti.accounting.entities.AccountingClosingEntity;
import com.sti.accounting.entities.AccountingPeriodEntity;
import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.entities.ControlAccountBalancesEntity;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.IAccountingClosingRepository;
import com.sti.accounting.repositories.IAccountingPeriodRepository;
import com.sti.accounting.repositories.IBalancesRepository;
import com.sti.accounting.repositories.IControlAccountBalancesRepository;
import com.sti.accounting.utils.PeriodStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AccountingClosingService {

    private static final Logger logger = LoggerFactory.getLogger(AccountingClosingService.class);

    private final IAccountingClosingRepository accountingClosingRepository;
    private final AccountingPeriodService accountingPeriodService;
    private final GeneralBalanceService generalBalanceService;
    private final IncomeStatementService incomeStatementService;
    private final IAccountingPeriodRepository accountingPeriodRepository;
    private final BalancesService balancesService;
    private final IBalancesRepository iBalancesRepository;
    private final IControlAccountBalancesRepository controlAccountBalancesRepository;

    public AccountingClosingService(IAccountingClosingRepository accountingClosingRepository, AccountingPeriodService accountingPeriodService, GeneralBalanceService generalBalanceService, IncomeStatementService incomeStatementService, IAccountingPeriodRepository accountingPeriodRepository, BalancesService balancesService, IBalancesRepository iBalancesRepository, IControlAccountBalancesRepository controlAccountBalancesRepository) {
        this.accountingClosingRepository = accountingClosingRepository;
        this.accountingPeriodService = accountingPeriodService;
        this.generalBalanceService = generalBalanceService;
        this.incomeStatementService = incomeStatementService;
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.balancesService = balancesService;
        this.iBalancesRepository = iBalancesRepository;
        this.controlAccountBalancesRepository = controlAccountBalancesRepository;
    }

    public List<AccountingClosingResponse> getAllAccountingClosing() {
        return this.accountingClosingRepository.findAll().stream().map(this::toResponse).toList();
    }


    public AccountingClosingResponse getDetailAccountingClosing() {
        logger.info("Generating detail accounting closing");

        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();

        AccountingClosingResponse accountingClosingResponse = new AccountingClosingResponse();

        accountingClosingResponse.setPeriodName(activePeriod.getPeriodName());
        accountingClosingResponse.setTypePeriod(activePeriod.getClosureType());
        accountingClosingResponse.setStartPeriod(activePeriod.getStartPeriod());
        accountingClosingResponse.setEndPeriod(activePeriod.getEndPeriod());

        // Obtener el balance general
        List<GeneralBalanceResponse> balanceResponses = generalBalanceService.getBalanceGeneral(activePeriod.getId());

        // Calcular totales
        BigDecimal totalAssets = balanceResponses.stream()
                .filter(item -> "ACTIVO".equals(item.getCategory()))
                .map(GeneralBalanceResponse::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLiabilities = balanceResponses.stream()
                .filter(item -> "PASIVO".equals(item.getCategory()))
                .map(GeneralBalanceResponse::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCapital = balanceResponses.stream()
                .filter(item -> "PATRIMONIO".equals(item.getCategory()))
                .map(GeneralBalanceResponse::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Asignar totales al response
        accountingClosingResponse.setTotalAssets(totalAssets);
        accountingClosingResponse.setTotalLiabilities(totalLiabilities);
        accountingClosingResponse.setTotalCapital(totalCapital);

        // Obtener el estado de resultados
        List<IncomeStatementResponse> incomeStatementResponses = incomeStatementService.getIncomeStatement(activePeriod.getId());

        // Calcular totales de ingresos y gastos
        BigDecimal totalIncome = incomeStatementResponses.stream()
                .filter(item -> "C".equals(item.getTypicalBalance()))
                .map(IncomeStatementResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpenses = incomeStatementResponses.stream()
                .filter(item -> "D".equals(item.getTypicalBalance()))
                .map(IncomeStatementResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Asignar totales de ingresos y gastos al response
        accountingClosingResponse.setTotalIncome(totalIncome);
        accountingClosingResponse.setTotalExpenses(totalExpenses);

        // Calcular el ingreso neto
        BigDecimal netIncome = incomeStatementService.getNetProfit(incomeStatementResponses);
        accountingClosingResponse.setNetIncome(netIncome);

        return accountingClosingResponse;
    }

    public void closeAccountingPeriod() {
        logger.info("Closing accounting period");

        // Obtener el período contable activo
        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();
        logger.info("Active accounting period ID: {}", activePeriod.getId());

        // Obtener los balances de cuentas para el período contable activo
        List<ControlAccountBalancesEntity> accountBalances = controlAccountBalancesRepository.findAllByAccountingPeriodId(activePeriod.getId());

        // Verificar si hay balances
        if (accountBalances.isEmpty()) {
            logger.warn("No account balances found for the active accounting period.");
            return;
        }

        // Agrupar los balances por accountId
        Map<Long, List<ControlAccountBalancesEntity>> groupedBalances = accountBalances.stream()
                .collect(Collectors.groupingBy(ControlAccountBalancesEntity::getAccountId));

        // Iterar sobre cada grupo de balances
        for (Map.Entry<Long, List<ControlAccountBalancesEntity>> entry : groupedBalances.entrySet()) {
            Long accountId = entry.getKey();
            List<ControlAccountBalancesEntity> balancesForAccount = entry.getValue();

            logger.info("Processing account balance for account ID: {}", accountId);

            // Inactivar el balance existente
            inactivateExistingBalances(accountId);

            // Crear un nuevo balance basado en los balances agrupados
            createNewBalance(balancesForAccount);
            logger.info("Created new balance for account ID: {}", accountId);
        }

        // Obtener los detalles del cierre contable
        AccountingClosingResponse closingDetails = getDetailAccountingClosing();

        // Crear registro de cierre contable
        AccountingClosingEntity closingEntity = new AccountingClosingEntity();
        closingEntity.setAccountingPeriod(activePeriod);
        closingEntity.setStartPeriod(closingDetails.getStartPeriod());
        closingEntity.setEndPeriod(closingDetails.getEndPeriod());
        closingEntity.setTotalAssets(closingDetails.getTotalAssets());
        closingEntity.setTotalLiabilities(closingDetails.getTotalLiabilities());
        closingEntity.setTotalCapital(closingDetails.getTotalCapital());
        closingEntity.setTotalIncome(closingDetails.getTotalIncome());
        closingEntity.setTotalExpenses(closingDetails.getTotalExpenses());
        closingEntity.setNetIncome(closingDetails.getNetIncome());

        accountingClosingRepository.save(closingEntity);
        logger.info("Accounting closing saved for period ID: {}", activePeriod.getId());

        // Cerrar el período contable actual
        closeActivePeriod(activePeriod);
        logger.info("Accounting period ID {} has been closed.", activePeriod.getId());

        // Activar el siguiente período contable
        activateNextPeriod(activePeriod);
    }

    private void activateNextPeriod(AccountingPeriodEntity currentPeriod) {
        // Obtener el siguiente período basado en el order
        AccountingPeriodEntity nextPeriod = accountingPeriodRepository.findByPeriodOrder(currentPeriod.getPeriodOrder() + 1);

        if (nextPeriod != null) {
            // Cambiar el estado del siguiente periodo a 'ACTIVE'
            nextPeriod.setPeriodStatus(PeriodStatus.ACTIVE);
            accountingPeriodRepository.save(nextPeriod);
            logger.info("Accounting period ID {} has been activated.", nextPeriod.getId());
        } else {
            logger.warn("No next period found to activate.");
        }
    }

    private void closeActivePeriod(AccountingPeriodEntity activePeriod) {
        activePeriod.setPeriodStatus(PeriodStatus.CLOSED);
        accountingPeriodRepository.save(activePeriod);
    }

    private void inactivateExistingBalances(Long accountId) {
        List<BalancesEntity> existingBalances = iBalancesRepository.findByAccountId(accountId);
        for (BalancesEntity existingBalance : existingBalances) {
            existingBalance.setIsCurrent(false);
            iBalancesRepository.save(existingBalance);
        }
    }

    private void createNewBalance(List<ControlAccountBalancesEntity> accountBalances) {
        BalancesRequest balancesRequest = new BalancesRequest();

        // Suponiendo que todas las entradas en accountBalances pertenecen a la misma cuenta
        if (!accountBalances.isEmpty()) {
            Long accountId = accountBalances.get(0).getAccountId();
            balancesRequest.setAccountId(accountId);

            // Obtener el balance actual de la cuenta
            BalancesEntity balancesEntity = this.iBalancesRepository.findMostRecentBalanceByAccountId(accountId);

            logger.info("balancesEntity: {}", balancesEntity);

            BigDecimal totalDebit = BigDecimal.ZERO;
            BigDecimal totalCredit = BigDecimal.ZERO;

            // Sumarizar los débitos y créditos de accountBalances
            for (ControlAccountBalancesEntity accountBalance : accountBalances) {
                if (accountBalance.getDebit() != null) {
                    totalDebit = totalDebit.add(accountBalance.getDebit());
                }
                if (accountBalance.getCredit() != null) {
                    totalCredit = totalCredit.add(accountBalance.getCredit());
                }
            }

            logger.info("Total Debit: {}", totalDebit);
            logger.info("Total Credit: {}", totalCredit);

            // Si balancesEntity no es nulo, sumar el balance actual al total correspondiente
            if (balancesEntity != null) {
                if ("D".equals(balancesEntity.getTypicalBalance())) {
                    totalDebit = totalDebit.add(balancesEntity.getInitialBalance());
                } else if ("C".equals(balancesEntity.getTypicalBalance())) {
                    totalCredit = totalCredit.add(balancesEntity.getInitialBalance());
                }
            }

            // Calcular el balance inicial
            BigDecimal initialBalance = totalDebit.subtract(totalCredit);

            // Determinar el balance típico
            if (totalDebit.compareTo(totalCredit) > 0) {
                balancesRequest.setTypicalBalance("D");
            } else {
                balancesRequest.setTypicalBalance("C");
            }

            logger.info("Initial Balance: {}", initialBalance);

            // Establecer el balance inicial
            balancesRequest.setInitialBalance(initialBalance);
        } else {
            logger.warn("No account balances provided for creating a new balance.");
            return;
        }

        balancesRequest.setIsCurrent(true);
        balancesService.createBalance(balancesRequest);
        logger.info("New balance created for account ID: {}", balancesRequest.getAccountId());
    }

    private AccountingClosingResponse toResponse(AccountingClosingEntity accountingClosingEntity) {
        AccountingClosingResponse accountingClosingResponse = new AccountingClosingResponse();

        accountingClosingResponse.setAccountingPeriodId(accountingClosingEntity.getAccountingPeriod().getId());
        accountingClosingResponse.setStartPeriod(accountingClosingEntity.getStartPeriod());
        accountingClosingResponse.setEndPeriod(accountingClosingEntity.getEndPeriod());
        accountingClosingResponse.setTotalAssets(accountingClosingEntity.getTotalAssets());
        accountingClosingResponse.setTotalLiabilities(accountingClosingEntity.getTotalLiabilities());
        accountingClosingResponse.setTotalCapital(accountingClosingEntity.getTotalCapital());
        accountingClosingResponse.setTotalIncome(accountingClosingEntity.getTotalIncome());
        accountingClosingResponse.setTotalExpenses(accountingClosingEntity.getTotalExpenses());
        accountingClosingResponse.setNetIncome(accountingClosingEntity.getNetIncome());
        return accountingClosingResponse;

    }
}