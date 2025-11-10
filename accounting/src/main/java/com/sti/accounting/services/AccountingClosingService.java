package com.sti.accounting.services;

import com.sti.accounting.entities.*;
import com.sti.accounting.models.*;
import com.sti.accounting.reports.ReportPdfGenerator;
import com.sti.accounting.repositories.*;
import com.sti.accounting.utils.PeriodStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private final AuthService authService;
    private final ICompanyRepository companyRepository;

    public AccountingClosingService(IAccountingClosingRepository accountingClosingRepository, AccountingPeriodService accountingPeriodService, GeneralBalanceService generalBalanceService, IncomeStatementService incomeStatementService, IAccountingPeriodRepository accountingPeriodRepository, BalancesService balancesService, IBalancesRepository iBalancesRepository, IControlAccountBalancesRepository controlAccountBalancesRepository, ReportPdfGenerator reportPdfGenerator, AuthService authService, ICompanyRepository companyRepository) {
        this.accountingClosingRepository = accountingClosingRepository;
        this.accountingPeriodService = accountingPeriodService;
        this.generalBalanceService = generalBalanceService;
        this.incomeStatementService = incomeStatementService;
        this.accountingPeriodRepository = accountingPeriodRepository;
        this.balancesService = balancesService;
        this.iBalancesRepository = iBalancesRepository;
        this.controlAccountBalancesRepository = controlAccountBalancesRepository;
        this.reportPdfGenerator = reportPdfGenerator;
        this.authService = authService;
        this.companyRepository = companyRepository;
    }

    public List<AccountingClosingResponse> getAllAccountingClosing() {
        String tenantId = authService.getTenantId();

        return this.accountingClosingRepository.findAll().stream().filter(closing -> closing.getTenantId().equals(tenantId)).map(this::toResponse).toList();
    }

    public AccountingClosingResponse getDetailAccountingClosing() {
        logger.info("Generating detail accounting closing");

        AccountingPeriodEntity activePeriod;
        AccountingPeriodEntity annualPeriod = accountingPeriodService.getAnnualPeriod();

        try {
            activePeriod = accountingPeriodService.getActivePeriod();
        } catch (ResponseStatusException e) {
            logger.warn("No active accounting period found: {}", e.getMessage());
            // Si no hay un período activo, usar el período anual
            activePeriod = annualPeriod;
        }

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

        // Save the accounting closing record
        saveAccountingClosing(activePeriod);

        // Process balances for the active accounting period
        processBalances(activePeriod);

        // Close the active accounting period
        closeActivePeriod(activePeriod);

        // Activate or create the next accounting period
        activateNextPeriod(activePeriod, newClosureType);

    }

    private void activateNextPeriod(AccountingPeriodEntity currentPeriod, String newClosureType) {
        int currentYear = LocalDate.now().getYear();
        String tenantId = authService.getTenantId();

        AccountingPeriodEntity nextPeriod = accountingPeriodRepository
                .findByClosureTypeAndPeriodOrderForYear(newClosureType, currentPeriod.getPeriodOrder() + 1, currentYear, tenantId);

        if (nextPeriod != null) {
            nextPeriod.setPeriodStatus(PeriodStatus.ACTIVE);
            accountingPeriodRepository.save(nextPeriod);
            logger.info("Accounting period ID: {} has been activated.", nextPeriod.getId());
        } else {
            logger.warn("No next period found. New periods will not be created.");

        }
    }

    private void processBalances(AccountingPeriodEntity activePeriod) {
        String tenantId = authService.getTenantId();

        List<ControlAccountBalancesEntity> accountBalances = controlAccountBalancesRepository.findAllByAccountingPeriodIdAndTenantId(activePeriod.getId(), tenantId);
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

    //ToDo: Revisar la generacion del PDF generado
    private void saveAccountingClosing(AccountingPeriodEntity activePeriod) {
        String tenantId = authService.getTenantId();

        CompanyEntity company = companyRepository.findByTenantId(tenantId);

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
        closingEntity.setTenantId(tenantId);

        // Generar el PDF y guardar su contenido como byte[]
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            reportPdfGenerator.generateReportPdf(os, company);
            byte[] pdf = os.toByteArray();
            logger.info("PDF generado en memoria: {} bytes", (pdf != null ? pdf.length : 0));
            closingEntity.setClosureReportPdf(pdf);
        } catch (Exception e) {
            logger.error("Error al generar el PDF de cierre", e);
            // Opcional: relanzar para hacer rollback:
             throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el PDF", e);
        }

        AccountingClosingEntity saved = accountingClosingRepository.save(closingEntity);
        logger.info("Closing saved id={}, pdfBytes={}",
                saved.getId(),
                (saved.getClosureReportPdf() != null ? saved.getClosureReportPdf().length : 0));
    }

    private void closeActivePeriod(AccountingPeriodEntity activePeriod) {
        activePeriod.setPeriodStatus(PeriodStatus.CLOSED);
        accountingPeriodRepository.save(activePeriod);
    }

    private void inactivateExistingBalances(Long accountId) {
        List<BalancesEntity> existingBalances = iBalancesRepository.findByAccountId(accountId);
        for (BalancesEntity existingBalance : existingBalances) {
            existingBalance.setIsCurrent(false);
            existingBalance.setClosingDate(LocalDateTime.now());
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

            if (totalDebit.compareTo(totalCredit) > 0) {
                balancesRequest.setTypicalBalance("D");
                balancesRequest.setInitialBalance(totalDebit.subtract(totalCredit));
            } else {
                balancesRequest.setTypicalBalance("C");
                balancesRequest.setInitialBalance(totalCredit.subtract(totalDebit));
            }
        }

        balancesRequest.setIsCurrent(true);
        balancesService.createBalance(balancesRequest);
    }

    public AccountingClosingResponse getAccountingClosingById(Long id) {
        AccountingClosingEntity closingEntity = accountingClosingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Accounting closing not found with id: " + id));

        return toResponse(closingEntity);
    }

    private AccountingClosingResponse toResponse(AccountingClosingEntity accountingClosingEntity) {
        AccountingClosingResponse accountingClosingResponse = new AccountingClosingResponse();

        accountingClosingResponse.setId(accountingClosingEntity.getId());
        accountingClosingResponse.setAccountingPeriodId(accountingClosingEntity.getAccountingPeriod().getId());
        accountingClosingResponse.setPeriodName(accountingClosingEntity.getAccountingPeriod().getPeriodName());
        accountingClosingResponse.setTypePeriod(accountingClosingEntity.getAccountingPeriod().getClosureType());
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


    //Cierre anual
    public void performAnnualClosing(String newClosureType) {
        logger.info("Iniciando proceso de cierre anual");

        // Obtener todos los períodos del año actual
        List<AccountingPeriodEntity> yearPeriods = accountingPeriodService.getClosedPeriods();

        // Verificar que todos los períodos, excepto el anual, estén cerrados
        boolean allPeriodsClosed = yearPeriods.stream()
                .allMatch(period -> period.getPeriodStatus() == PeriodStatus.CLOSED || period.getIsAnnual());

        if (!allPeriodsClosed) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "All periods must be closed before performing the annual closing."
            );
        }

        // Obtener el período anual
        AccountingPeriodEntity annualPeriod = accountingPeriodService.getAnnualPeriod();
        if (annualPeriod == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "There is no annual period available to perform the annual closing"
            );
        }

        try {
            // Generar el PDF anual con todos los períodos
            ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
            generateAnnualReport(pdfOutputStream, yearPeriods);

            // Crear el registro de cierre anual
            AccountingClosingEntity annualClosing = new AccountingClosingEntity();
            annualClosing.setAccountingPeriod(annualPeriod);
            annualClosing.setStartPeriod(yearPeriods.get(0).getStartPeriod());
            annualClosing.setEndPeriod(annualPeriod.getEndPeriod());
            annualClosing.setClosureReportPdf(pdfOutputStream.toByteArray());

            // Calcular totales anuales
            calculateAnnualTotals(annualClosing, yearPeriods);

            // Guardar el cierre anual
            accountingClosingRepository.save(annualClosing);

            // Cerrar el período anual
            closeAnnualPeriod(annualPeriod);

            // Crear el nuevo período anual para el siguiente año
            createNextYearPeriod(annualPeriod);

            // Crear los nuevos periodos para el siguiente año
            createNextYearPeriods(annualPeriod, newClosureType);

        } catch (Exception e) {
            logger.error("Error during annual closing", e);

        }
    }

    public void closeAnnualPeriod(AccountingPeriodEntity annualPeriod) {
        if (annualPeriod == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The annual period cannot be null");
        }

        annualPeriod.setPeriodStatus(PeriodStatus.CLOSED);
        accountingPeriodRepository.save(annualPeriod);
    }


    private void calculateAnnualTotals(AccountingClosingEntity annualClosing, List<AccountingPeriodEntity> yearPeriods) {
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalCapital = BigDecimal.ZERO;
        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpenses = BigDecimal.ZERO;

        for (AccountingPeriodEntity period : yearPeriods) {
            List<GeneralBalanceResponse> balances = generalBalanceService.getBalanceGeneral(period.getId());
            List<IncomeStatementResponse> incomeStatement = incomeStatementService.getIncomeStatement(period.getId());

            // Acumular totales del balance general
            for (GeneralBalanceResponse balance : balances) {
                switch (balance.getCategory()) {
                    case "ACTIVO":
                        totalAssets = totalAssets.add(balance.getBalance());
                        break;
                    case "PASIVO":
                        totalLiabilities = totalLiabilities.add(balance.getBalance());
                        break;
                    case "PATRIMONIO":
                        totalCapital = totalCapital.add(balance.getBalance());
                        break;
                    default:
                        break;
                }
            }

            // Acumular totales del estado de resultados
            for (IncomeStatementResponse item : incomeStatement) {
                if ("C".equals(item.getTypicalBalance())) {
                    totalIncome = totalIncome.add(item.getAmount());
                } else if ("D".equals(item.getTypicalBalance())) {
                    totalExpenses = totalExpenses.add(item.getAmount());
                }
            }
        }

        annualClosing.setTotalAssets(totalAssets);
        annualClosing.setTotalLiabilities(totalLiabilities);
        annualClosing.setTotalCapital(totalCapital);
        annualClosing.setTotalIncome(totalIncome);
        annualClosing.setTotalExpenses(totalExpenses);
        annualClosing.setNetIncome(totalIncome.subtract(totalExpenses));
    }

    private void createNextYearPeriod(AccountingPeriodEntity currentPeriod) {
        LocalDateTime startOfNextYear = currentPeriod.getEndPeriod()
                .plusDays(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0);

        AccountingPeriodRequest request = new AccountingPeriodRequest();
        request.setStartPeriod(startOfNextYear);
        request.setPeriodName("Periodo Anual " + startOfNextYear.getYear());
        request.setClosureType("Anual");
        request.setDaysPeriod(365);
        request.setPeriodStatus(PeriodStatus.INACTIVE);
        request.setPeriodOrder(0);
        request.setIsAnnual(true);

        accountingPeriodService.createAccountingPeriod(request);
    }

    private void createNextYearPeriods(AccountingPeriodEntity currentPeriod, String newClosureType) {

        AccountingPeriodRequest request = new AccountingPeriodRequest();
        request.setStartPeriod(currentPeriod.getEndPeriod().plusDays(1));
        request.setClosureType(newClosureType);
        request.setPeriodName("Periodo " + newClosureType.substring(0, 1).toUpperCase() + newClosureType.substring(1).toLowerCase());
        request.setIsAnnual(false);

        AccountingPeriodResponse newPeriodResponse = accountingPeriodService.createAccountingPeriod(request);
        logger.info("Se creó un nuevo periodo contable con ID {}", newPeriodResponse.getId());

    }

    private void generateAnnualReport(OutputStream outputStream, List<AccountingPeriodEntity> yearPeriods) throws MalformedURLException {
        String tenantId = authService.getTenantId();

        CompanyEntity company = companyRepository.findByTenantId(tenantId);

        reportPdfGenerator.generateAnnualReportPdf(outputStream, yearPeriods,company);
    }
}