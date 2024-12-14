package com.sti.accounting.services;

import com.sti.accounting.entities.AccountingClosingEntity;
import com.sti.accounting.entities.AccountingPeriodEntity;
import com.sti.accounting.entities.BalancesEntity;
import com.sti.accounting.entities.ControlAccountBalancesEntity;
import com.sti.accounting.models.*;
import com.sti.accounting.reports.ReportPdfGenerator;
import com.sti.accounting.repositories.IAccountingClosingRepository;
import com.sti.accounting.repositories.IAccountingPeriodRepository;
import com.sti.accounting.repositories.IBalancesRepository;
import com.sti.accounting.repositories.IControlAccountBalancesRepository;
import com.sti.accounting.utils.PeriodStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    private final ReportPdfGenerator reportPdfGenerator;

    public AccountingClosingService(IAccountingClosingRepository accountingClosingRepository, AccountingPeriodService accountingPeriodService, GeneralBalanceService generalBalanceService, IncomeStatementService incomeStatementService, IAccountingPeriodRepository accountingPeriodRepository, BalancesService balancesService, IBalancesRepository iBalancesRepository, IControlAccountBalancesRepository controlAccountBalancesRepository, ReportPdfGenerator reportPdfGenerator) {
        this.accountingClosingRepository = accountingClosingRepository;
        this.accountingPeriodService = accountingPeriodService;
        this.generalBalanceService = generalBalanceService;
        this.incomeStatementService = incomeStatementService;
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.balancesService = balancesService;
        this.iBalancesRepository = iBalancesRepository;
        this.controlAccountBalancesRepository = controlAccountBalancesRepository;
        this.reportPdfGenerator = reportPdfGenerator;
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

    public void closeAccountingPeriod(String newClosureType) {
        logger.info("Closing accounting period with new closure type: {}", newClosureType);

        AccountingPeriodEntity activePeriod = accountingPeriodService.getActivePeriod();
        logger.info("Active accounting period ID: {}", activePeriod.getId());

        // Process balances for the active accounting period
        processBalances(activePeriod);

        // Save the accounting closing record
        saveAccountingClosing(activePeriod);

        // Close the active accounting period
        closeActivePeriod(activePeriod);

        // Activate or create the next accounting period
        activateOrCreateNextPeriod(activePeriod, newClosureType);

    }

    private void activateOrCreateNextPeriod(AccountingPeriodEntity currentPeriod, String newClosureType) {

        int currentYear = LocalDate.now().getYear();

        AccountingPeriodEntity nextPeriod = accountingPeriodRepository
                .findByClosureTypeAndPeriodOrderForYear(newClosureType, currentPeriod.getPeriodOrder() + 1,currentYear);


        if (nextPeriod != null) {
            nextPeriod.setPeriodStatus(PeriodStatus.ACTIVE);
            accountingPeriodRepository.save(nextPeriod);
            logger.info("El periodo contable ID {} ha sido activado.", nextPeriod.getId());

        } else {
            logger.warn("No se encontró un periodo siguiente. Creando nuevos periodos.");

            AccountingPeriodRequest request = new AccountingPeriodRequest();
            request.setStartPeriod(currentPeriod.getEndPeriod().plusDays(1));
            request.setClosureType(newClosureType);
            request.setPeriodName("Periodo " + newClosureType.substring(0, 1).toUpperCase() + newClosureType.substring(1).toLowerCase());
            request.setIsAnnual(false);

            AccountingPeriodResponse newPeriodResponse = accountingPeriodService.createAccountingPeriod(request);
            logger.info("Se creó un nuevo periodo contable con ID {}", newPeriodResponse.getId());

        }


    }

    private void processBalances(AccountingPeriodEntity activePeriod) {
        List<ControlAccountBalancesEntity> accountBalances = controlAccountBalancesRepository.findAllByAccountingPeriodId(activePeriod.getId());
        if (accountBalances.isEmpty()) {
            logger.warn("No account balances found for the active accounting period.");
            return;
        }

        Map<Long, List<ControlAccountBalancesEntity>> groupedBalances = accountBalances.stream()
                .collect(Collectors.groupingBy(ControlAccountBalancesEntity::getAccountId));

        for (Map.Entry<Long, List<ControlAccountBalancesEntity>> entry : groupedBalances.entrySet()) {
            Long accountId = entry.getKey();
            List<ControlAccountBalancesEntity> balancesForAccount = entry.getValue();
            logger.info("Processing account balance for account ID: {}", accountId);

            inactivateExistingBalances(accountId);
            createNewBalance(balancesForAccount);
        }
    }

    private void saveAccountingClosing(AccountingPeriodEntity activePeriod) {
        AccountingClosingResponse closingDetails = getDetailAccountingClosing();

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

        // Generar el PDF y guardar su contenido como byte[]
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            // Genera el PDF usando el generador
            reportPdfGenerator.generateReportPdf(outputStream);

            // Guarda los bytes del PDF en la entidad
            closingEntity.setClosureReportPdf(outputStream.toByteArray());
            logger.info("PDF generado y almacenado en la base de datos");
        } catch (Exception e) {
            logger.info("Error al generar el PDF");
        }

        accountingClosingRepository.save(closingEntity);
        logger.info("Accounting closing saved for period ID: {}", activePeriod.getId());
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

        if (!accountBalances.isEmpty()) {
            Long accountId = accountBalances.get(0).getAccountId();
            balancesRequest.setAccountId(accountId);

            BalancesEntity balancesEntity = iBalancesRepository.findMostRecentBalanceByAccountId(accountId);

            BigDecimal totalDebit = accountBalances.stream()
                    .map(ControlAccountBalancesEntity::getDebit)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalCredit = accountBalances.stream()
                    .map(ControlAccountBalancesEntity::getCredit)
                    .filter(java.util.Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (balancesEntity != null) {
                if ("D".equals(balancesEntity.getTypicalBalance())) {
                    totalDebit = totalDebit.add(balancesEntity.getInitialBalance());
                } else if ("C".equals(balancesEntity.getTypicalBalance())) {
                    totalCredit = totalCredit.add(balancesEntity.getInitialBalance());
                }
            }

            balancesRequest
                    .setTypicalBalance(totalDebit.compareTo(totalCredit) > 0 ? "D" : "C");
            balancesRequest.setInitialBalance(totalDebit.subtract(totalCredit));
        }

        balancesRequest.setIsCurrent(true);
        balancesService.createBalance(balancesRequest);
    }

    public AccountingClosingResponse getAccountingClosingById(Long id) {
        AccountingClosingEntity closingEntity = accountingClosingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cierre contable no encontrado con id: " + id));

        return toResponse(closingEntity);
    }

    private AccountingClosingResponse toResponse(AccountingClosingEntity accountingClosingEntity) {
        AccountingClosingResponse accountingClosingResponse = new AccountingClosingResponse();

        accountingClosingResponse.setId(accountingClosingEntity.getId());
        accountingClosingResponse.setAccountingPeriodId(accountingClosingEntity.getAccountingPeriod().getId());
        accountingClosingResponse.setStartPeriod(accountingClosingEntity.getStartPeriod());
        accountingClosingResponse.setEndPeriod(accountingClosingEntity.getEndPeriod());
        accountingClosingResponse.setTotalAssets(accountingClosingEntity.getTotalAssets());
        accountingClosingResponse.setTotalLiabilities(accountingClosingEntity.getTotalLiabilities());
        accountingClosingResponse.setTotalCapital(accountingClosingEntity.getTotalCapital());
        accountingClosingResponse.setTotalIncome(accountingClosingEntity.getTotalIncome());
        accountingClosingResponse.setTotalExpenses(accountingClosingEntity.getTotalExpenses());
        accountingClosingResponse.setNetIncome(accountingClosingEntity.getNetIncome());
        accountingClosingResponse.setClosureReportPdf(accountingClosingEntity.getClosureReportPdf());
        return accountingClosingResponse;
    }
}