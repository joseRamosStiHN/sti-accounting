package com.sti.accounting;

import com.sti.accounting.entities.AccountCategoryEntity;
import com.sti.accounting.entities.AccountTypeEntity;
import com.sti.accounting.entities.DocumentEntity;
import com.sti.accounting.repositories.IAccountCategoryRepository;
import com.sti.accounting.repositories.IAccountTypeRepository;
import com.sti.accounting.repositories.IAccountingPeriodRepository;
import com.sti.accounting.repositories.IDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@SpringBootApplication()
public class AccountingApplication {

    private static final Logger logger = LoggerFactory.getLogger(AccountingApplication.class);

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
                        new DocumentEntity(4L, "Notas de Debito", "Notas de Debito", new HashSet<>()),
                        new DocumentEntity(5L, "Otros", "Otros", new HashSet<>())
                );
                document.saveAll(documents);
            }

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


            createNumberPdaTrigger(dataSource);

        };
    }

    private void createNumberPdaTrigger(DataSource dataSource) {
        String triggerCheckQuery = "SELECT COUNT(*) FROM information_schema.triggers WHERE trigger_name = 'INSERT_NUMBER_PDA'";

        String createTriggerQuery =
                "CREATE TRIGGER INSERT_NUMBER_PDA " +
                        "BEFORE INSERT ON transactions " +
                        "FOR EACH ROW " +
                        "BEGIN " +
                        "   DECLARE next_number INT; " +

                        "   IF EXISTS (SELECT 1 FROM pda_sequence WHERE tenant_id = NEW.tenant_id) THEN " +
                        "       SELECT last_number + 1 INTO next_number " +
                        "       FROM pda_sequence " +
                        "       WHERE tenant_id = NEW.tenant_id FOR UPDATE; " +

                        "       SET NEW.number_pda = next_number; " +

                        "       UPDATE pda_sequence " +
                        "       SET last_number = next_number " +
                        "       WHERE tenant_id = NEW.tenant_id; " +

                        "   ELSE " +
                        "       SET NEW.number_pda = 1; " +

                        "       INSERT INTO pda_sequence (tenant_id, last_number) " +
                        "       VALUES (NEW.tenant_id, 1); " +
                        "   END IF; " +
                        "END";

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            ResultSet resultSet = statement.executeQuery(triggerCheckQuery);
            if (resultSet.next() && resultSet.getInt(1) == 0) {
                statement.execute("DROP TRIGGER IF EXISTS INSERT_NUMBER_PDA"); //  por si hubo un intento fallido antes
                statement.execute(createTriggerQuery);
            }
        } catch (SQLException e) {
            logger.error("error generated createNumberPdaTrigger:", e);
        }
    }

}
