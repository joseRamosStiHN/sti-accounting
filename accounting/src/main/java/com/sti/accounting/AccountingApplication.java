package com.sti.accounting;

import com.sti.accounting.entities.AccountCategoryEntity;
import com.sti.accounting.entities.DocumentEntity;
import com.sti.accounting.repositories.IAccountCategoryRepository;
import com.sti.accounting.repositories.IDocumentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;


@SpringBootApplication()
public class AccountingApplication {

	public static void main(String[] args) {
		SpringApplication.run(AccountingApplication.class, args);
	}


	@Bean
	CommandLineRunner seedCategories(IAccountCategoryRepository repository, IDocumentRepository document) {
		return args -> {
			long count = repository.count();
			if (count == 0) {
				List<AccountCategoryEntity> categories = Arrays.asList(
						new AccountCategoryEntity(1L, "Balance General", "Agrupa las cuentas de Balance General", new HashSet<>()),
						new AccountCategoryEntity(2L, "Estado de Resultados", "Agrupa las cuentas de resultados", new HashSet<>())
				);
				repository.saveAll(categories);
			}
			long documentCount = document.count();
			if(documentCount == 0){
				List<DocumentEntity> documents = Arrays.asList(
						new DocumentEntity(1L, "Factura de Clientes", "Factura de clientes", new HashSet<>()),
						new DocumentEntity(2L, "Factura de Proveedores", "Factura de proveedores", new HashSet<>()),
						new DocumentEntity(3L, "Notas de Credito", "Notas de Credito", new HashSet<>()),
						new DocumentEntity(4L, "Notas de Debito", "Notas de Debito", new HashSet<>())
				);
				document.saveAll(documents);
			}
		};
	}

//	@Bean
//	CommandLineRunner triggerNumberPda(DataSource dataSource) {
//		return args -> {
//			try (Connection conn = dataSource.getConnection();
//				 Statement stmt = conn.createStatement()) {
//				stmt.execute("CREATE TRIGGER insert_num_pda BEFORE INSERT ON transactions FOR EACH ROW BEGIN SET NEW.number_pda = (SELECT COALESCE(MAX(number_pda), 0) + 1 FROM transactions); END;");
//			} catch (SQLException e) {
//				e.printStackTrace();
//			}
//		};
//	}




}
