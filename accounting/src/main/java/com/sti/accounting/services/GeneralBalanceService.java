package com.sti.accounting.services;

import com.sti.accounting.entities.*;
import com.sti.accounting.models.*;
import com.sti.accounting.repositories.IAccountRepository;
import com.sti.accounting.utils.Status;
import com.sti.accounting.utils.TenantContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GeneralBalanceService {

    private static final Logger logger = LoggerFactory.getLogger(GeneralBalanceService.class);

    private final IAccountRepository iAccountRepository;
    private final ControlAccountBalancesService controlAccountBalancesService;
    private final IncomeStatementService incomeStatementService;
    private final AccountingPeriodService accountingPeriodService;

    public GeneralBalanceService(IAccountRepository iAccountRepository, ControlAccountBalancesService controlAccountBalancesService, IncomeStatementService incomeStatementService, AccountingPeriodService accountingPeriodService) {
        this.iAccountRepository = iAccountRepository;
        this.controlAccountBalancesService = controlAccountBalancesService;
        this.incomeStatementService = incomeStatementService;
        this.accountingPeriodService = accountingPeriodService;
    }

    private String getTenantId() {
        return TenantContext.getCurrentTenant();
    }

    @Transactional
    public List<GeneralBalanceResponse> getBalanceGeneral(Long periodId) {
        logger.info("Generating balance general");
        String tenantId = getTenantId();

        // Realiza el filtro por las cuentas activas
        List<AccountEntity> accounts = iAccountRepository.findAll().stream()
                .filter(account -> account.getAccountCategory().getName().equalsIgnoreCase("Balance General") && account.getStatus() == Status.ACTIVO && account.getTenantId().equals(tenantId))
                .toList();

        List<GeneralBalanceResponse> response = new ArrayList<>();

        // Si periodId es null, se obtienen los balances para todos los periodos
        for (AccountEntity account : accounts) {
            ControlAccountBalancesEntity sumViewEntity;

            if (periodId != null) {
                // Obtener el periodo específico
                AccountingPeriodResponse period = accountingPeriodService.getById(periodId);
                if (period != null) {
                    // Obtener el inicio y el final del periodo mensual
                    LocalDate startPeriod = period.getStartPeriod().toLocalDate();
                    LocalDate endPeriod = period.getEndPeriod().toLocalDate();

                    List<ControlAccountBalancesEntity> balances = controlAccountBalancesService.getControlAccountBalancesForPeriodAndMonth(account.getId(), period.getId(), startPeriod, endPeriod);
                    sumViewEntity = combineBalances(balances);
                } else {
                    // Manejar el caso en que el periodo no existe
                    List<ControlAccountBalancesEntity> balances = controlAccountBalancesService.getControlAccountBalancesForAllPeriods(account.getId());
                    sumViewEntity = combineBalances(balances);
                }
            } else {
                // Si periodId es null, obtener balances para todos los periodos
                List<ControlAccountBalancesEntity> balances = controlAccountBalancesService.getControlAccountBalancesForAllPeriods(account.getId());
                sumViewEntity = combineBalances(balances);
            }

            GeneralBalanceResponse generalBalanceResponse = createGeneralBalanceResponse(account, sumViewEntity);
            response.add(generalBalanceResponse);
        }

        // Verificar si la cuenta existe y actualizar su balance
        List<IncomeStatementResponse> incomeStatementResponse = incomeStatementService.getIncomeStatement(periodId);
        Optional<GeneralBalanceResponse> optionalAccount = response.stream()
                .filter(item -> item.getAccountName().equals("UTILIDAD O PERDIDA NETA DEL EJERCICIO"))
                .findFirst();

        if (optionalAccount.isPresent()) {
            optionalAccount.get().setBalance(incomeStatementService.getNetProfit(incomeStatementResponse));
        } else {
            GeneralBalanceResponse netProfitResponse = new GeneralBalanceResponse();
            netProfitResponse.setAccountId(0L);
            netProfitResponse.setAccountName("UTILIDAD O PERDIDA NETA DEL EJERCICIO");
            netProfitResponse.setParentId(null);
            netProfitResponse.setCategory("PATRIMONIO");
            netProfitResponse.setBalance(incomeStatementService.getNetProfit(incomeStatementResponse));
            netProfitResponse.setRoot(true);
            response.add(netProfitResponse);
        }

        return response;
    }

    private GeneralBalanceResponse createGeneralBalanceResponse(AccountEntity account, ControlAccountBalancesEntity sumViewEntity) {
        GeneralBalanceResponse item = new GeneralBalanceResponse();
        item.setAccountId(account.getId());
        item.setAccountName(account.getDescription());
        item.setParentId(account.getParent() != null ? account.getParent().getId() : null);

        String category = getCategory(account);
        item.setCategory(category);

        // Si sumViewEntity es null, inicializarlo
        if (sumViewEntity == null) {
            sumViewEntity = new ControlAccountBalancesEntity();
            sumViewEntity.setDebit(BigDecimal.valueOf(0));
            sumViewEntity.setCredit(BigDecimal.valueOf(0));
        }

        BigDecimal balance = getBalanceWhitInitialBalance(sumViewEntity, account);
        item.setBalance(balance);
        item.setRoot(account.getParent() == null);

        return item;
    }

    private ControlAccountBalancesEntity combineBalances(List<ControlAccountBalancesEntity> balances) {
        ControlAccountBalancesEntity combined = new ControlAccountBalancesEntity();
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        for (ControlAccountBalancesEntity balance : balances) {
            BigDecimal debitValue = balance.getDebit() != null ? balance.getDebit() : BigDecimal.valueOf(0);
            BigDecimal creditValue = balance.getCredit() != null ? balance.getCredit() : BigDecimal.valueOf(0);

            totalDebit = totalDebit.add(debitValue);
            totalCredit = totalCredit.add(creditValue);
        }

        combined.setDebit(totalDebit);
        combined.setCredit(totalCredit);
        return combined;
    }

    private String getCategory(AccountEntity account) {
        String[] mainCategories = {"ACTIVO", "PASIVO", "PATRIMONIO"};
        AccountEntity current = account;
        while (current != null) {
            for (String mainCategory : mainCategories) {
                if (current.getDescription().toUpperCase().contains(mainCategory)) {
                    return mainCategory;
                }
            }
            current = current.getParent();
        }
        return "";
    }


    private BigDecimal getBalanceWhitInitialBalance(ControlAccountBalancesEntity sumViewEntity, AccountEntity account) {

        // Calcular los saldos iniciales solo una vez
        BigDecimal initialBalanceDebit = account.getBalances().stream()
                .filter(balancesEntity ->
                        balancesEntity.getTypicalBalance().equalsIgnoreCase("D") &&
                                Boolean.TRUE.equals(balancesEntity.getIsCurrent())
                )
                .map(BalancesEntity::getInitialBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal initialBalanceCredit = account.getBalances().stream()
                .filter(balancesEntity ->
                        balancesEntity.getTypicalBalance().equalsIgnoreCase("C") &&
                                Boolean.TRUE.equals(balancesEntity.getIsCurrent())
                )
                .map(BalancesEntity::getInitialBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Si sumViewEntity es null, devolver solo el balance inicial
        if (sumViewEntity == null) {
            return initialBalanceDebit.subtract(initialBalanceCredit);
        }

        // Si sumViewEntity no es null, continuar con el cálculo
        BigDecimal debit = sumViewEntity.getDebit() != null ? sumViewEntity.getDebit() : BigDecimal.ZERO;
        debit = initialBalanceDebit.add(debit);

        BigDecimal credit = sumViewEntity.getCredit() != null ? sumViewEntity.getCredit() : BigDecimal.ZERO;
        credit = initialBalanceCredit.add(credit);

        if (account.getTypicalBalance().equalsIgnoreCase("D")) {
            return debit.subtract(credit);
        } else if (account.getTypicalBalance().equalsIgnoreCase("C")) {
            return credit.subtract(debit);
        } else {
            return debit.subtract(credit);
        }
    }
}