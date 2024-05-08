package com.sti.accounting;

import com.sti.accounting.entities.AccountCategoryEntity;
import com.sti.accounting.entities.DocumentEntity;
import com.sti.accounting.repositories.IAccountCategoryRepository;
import com.sti.accounting.repositories.IDocumentRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

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
						new DocumentEntity(4L, "Notas de Debito", "Notas de Credito", new HashSet<>())
				);
				document.saveAll(documents);
			}
		};
	}



}
