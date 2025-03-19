package com.sti.accounting;

import com.sti.accounting.entities.AccountCategoryEntity;
import com.sti.accounting.entities.AccountTypeEntity;
import com.sti.accounting.entities.AccountingPeriodEntity;
import com.sti.accounting.entities.DocumentEntity;
import com.sti.accounting.repositories.IAccountCategoryRepository;
import com.sti.accounting.repositories.IAccountTypeRepository;
import com.sti.accounting.repositories.IAccountingPeriodRepository;
import com.sti.accounting.repositories.IDocumentRepository;
import com.sti.accounting.utils.TenantContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static com.sti.accounting.utils.PeriodStatus.INACTIVE;


@SpringBootApplication()
public class AccountingApplication {

    public static void main(String[] args) {
        SpringApplication.run(AccountingApplication.class, args);
    }

    @Bean
    CommandLineRunner seedCategories(IAccountCategoryRepository repository, IDocumentRepository document, IAccountTypeRepository accountType, IAccountingPeriodRepository accountingPeriodRepository, DataSource dataSource) {
        return args -> {
            long count = repository.count();
            if (count == 0) {
                List<AccountCategoryEntity> categories = Arrays.asList(
                        new AccountCategoryEntity(1L, "Balance General", "Agrupa las cuentas de Balance General", new HashSet<>()),
                        new AccountCategoryEntity(2L, "Estado de Resultados", "Agrupa las cuentas de resultados", new HashSet<>()),
                        new AccountCategoryEntity(3L, "Impuestos", "Agrupa las cuentas de impuesto", new HashSet<>())

                );
                repository.saveAll(categories);
            }
            long documentCount = document.count();
            if (documentCount == 0) {
                List<DocumentEntity> documents = Arrays.asList(
                        new DocumentEntity(1L, "Factura de Clientes", "Factura de clientes", new HashSet<>()),
                        new DocumentEntity(2L, "Factura de Proveedores", "Factura de proveedores", new HashSet<>()),
                        new DocumentEntity(3L, "Notas de Credito", "Notas de Credito", new HashSet<>()),
                        new DocumentEntity(4L, "Notas de Debito", "Notas de Debito", new HashSet<>())
                );
                document.saveAll(documents);
            }
            long accountTypeCount = accountType.count();
            if (accountTypeCount == 0) {
                List<AccountTypeEntity> accountsType = Arrays.asList(
                        new AccountTypeEntity(1L, "Ingresos", "Tipo de cuenta para ingresos.", new HashSet<>()),
                        new AccountTypeEntity(2L, "Gastos", "Tipo de cuenta para gastos.", new HashSet<>()),
                        new AccountTypeEntity(3L, "Efectivo", "Tipo de cuenta para efectivo.", new HashSet<>()),
                        new AccountTypeEntity(4L, "Bancos", "Tipo de cuenta para bancos.", new HashSet<>()),
                        new AccountTypeEntity(5L, "Varios", "Tipo de cuenta para varios.", new HashSet<>()),
                        new AccountTypeEntity(6L, "Patrimonio", "Tipo de cuenta para patrimonio.", new HashSet<>()),
                        new AccountTypeEntity(7L, "Compras", "Tipo de cuenta para compras.", new HashSet<>())
                );
                accountType.saveAll(accountsType);
            }

            long accountingPeriod = accountingPeriodRepository.count();
            LocalDateTime startOfYear = LocalDateTime.of(Year.now().getValue(), 1, 1, 0, 0);
            LocalDateTime endOfYear = LocalDateTime.of(Year.now().getValue(), 12, 31, 23, 59, 59);
            String tenantId = TenantContext.getCurrentTenant();
            if (accountingPeriod == 0) {
                List<AccountingPeriodEntity> accountingPeriods = List.of(
                        new AccountingPeriodEntity(
                                null,
                                "Periodo Contable Anual",
                                "Anual",
                                startOfYear,
                                endOfYear,
                                365,
                                INACTIVE,
                                0,
                                true,
                                tenantId
                        )
                );
                accountingPeriodRepository.saveAll(accountingPeriods);
            }

            createNumberPdaTrigger(dataSource);

        };
    }

    private void createNumberPdaTrigger(DataSource dataSource) {
        String triggerCheckQuery = "SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_name = 'INSERT_NUMBER_PDA'";
        String createTriggerQuery = "CREATE TRIGGER INSERT_NUMBER_PDA " +
                "BEFORE INSERT ON TRANSACTIONS " +
                "FOR EACH ROW " +
                "BEGIN " +
                "SET NEW.NUMBER_PDA = (SELECT COALESCE(MAX(NUMBER_PDA), 0) + 1 FROM TRANSACTIONS); " +
                "END;";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            var resultSet = statement.executeQuery(triggerCheckQuery);
            if (resultSet.next() && resultSet.getInt(1) == 0) {
                statement.execute(createTriggerQuery);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
