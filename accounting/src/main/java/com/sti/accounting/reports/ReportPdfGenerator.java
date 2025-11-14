package com.sti.accounting.reports;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.*;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;
import com.sti.accounting.entities.AccountingPeriodEntity;
import com.sti.accounting.entities.CompanyEntity;
import com.sti.accounting.models.GeneralBalanceResponse;
import com.sti.accounting.models.IncomeStatementResponse;
import com.sti.accounting.models.TrialBalanceResponse;
import com.sti.accounting.services.*;
import com.sti.accounting.utils.IncomeStatementCalculator;
import com.sti.accounting.utils.IncomeStatementFormatter;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ReportPdfGenerator {

    private final GeneralBalanceService generalBalanceService;
    private final IncomeStatementService incomeStatementService;
    private final TrialBalanceService trialBalanceService;
    private final AccountingPeriodService accountingPeriodService;
    private final TaxSettingsService taxSettingsService;

    public ReportPdfGenerator(GeneralBalanceService generalBalanceService,
                              IncomeStatementService incomeStatementService,
                              TrialBalanceService trialBalanceService,
                              AccountingPeriodService accountingPeriodService, TaxSettingsService taxSettingsService) {
        this.generalBalanceService = generalBalanceService;
        this.incomeStatementService = incomeStatementService;
        this.trialBalanceService = trialBalanceService;
        this.accountingPeriodService = accountingPeriodService;
        this.taxSettingsService = taxSettingsService;
    }

    /**
     * Método principal que genera el PDF completo con todas las secciones del reporte.
     */
    public void generateReportPdf(OutputStream outputStream, CompanyEntity company) throws MalformedURLException {
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
        Document document = new Document(pdfDoc);

        // 1. Sección de "Reportes Financieros"
        addFinancialReportHeader(document, "Reportes Financieros", false, company);

        // 2. Sección de "Balanza de Comprobación"
        generateTrialBalanceSection(document);

        // 3. Sección de "Balance General"
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        generateBalanceSection(document, company);

        // 4. Sección de "Estado de Resultados"
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        generateIncomeStatementSection(document, company);

        document.close();
    }

    /**
     * Agrega el encabezado principal reutilizable con texto personalizado.
     */
    private void addFinancialReportHeader(Document document, String reportTitle, boolean isAnnual, CompanyEntity company)
            throws MalformedURLException {

        String periodRange;
        if (isAnnual) {
            periodRange = accountingPeriodService.getAnnualPeriod().getStartPeriod().toLocalDate()
                    + " al " + accountingPeriodService.getAnnualPeriod().getEndPeriod().toLocalDate();
        } else {
            periodRange = accountingPeriodService.getActivePeriod().getStartPeriod().toLocalDate()
                    + " al " + accountingPeriodService.getActivePeriod().getEndPeriod().toLocalDate();
        }

        Image logoImage = null;

        try {
            if (company.getCompanyLogo() != null && company.getCompanyLogo().length > 0) {

                // 🔹 OPCIONAL: si el logo es absurdo (> 2MB), no lo uso
                if (company.getCompanyLogo().length < 2_000_000) {
                    logoImage = new Image(ImageDataFactory.create(company.getCompanyLogo()))
                            .setAutoScale(true); // solo ajuste visual
                } else {
                    // loggear y no usar logo
                    System.out.println("Company logo too large: " + company.getCompanyLogo().length + " bytes");
                }

            } else {
                // Logo por defecto desde resources (asegúrate que el archivo sea pequeño)
                var logoUrl = getClass().getClassLoader().getResource("logo.jpg");
                if (logoUrl != null) {
                    logoImage = new Image(ImageDataFactory.create(logoUrl))
                            .setAutoScale(true);
                }
            }
        } catch (Exception ex) {
            // Si algo falla con el logo, NO rompemos el PDF
            System.err.println("Error loading logo image: " + ex.getMessage());
        }

        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1}))
                .useAllAvailableWidth();

        // Izquierda: logo (si existe)
        Cell leftCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER);
        if (logoImage != null) {
            leftCell.add(logoImage);
        }
        headerTable.addCell(leftCell);

        // Centro: textos
        Cell textCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER);
        textCell.add(new Paragraph(company.getCompanyName())
                .setBold()
                .setFontSize(20));
        textCell.add(new Paragraph(reportTitle)
                .setBold()
                .setFontSize(16));
        textCell.add(new Paragraph("Período: " + periodRange)
                .setBold()
                .setFontSize(12));
        headerTable.addCell(textCell);

        // Derecha: vacío
        headerTable.addCell(new Cell().setBorder(Border.NO_BORDER));

        document.add(headerTable);
        document.add(new Paragraph("\n"));
    }

    /**
     * Genera la sección "Balanza de Comprobación".
     */
    private void generateTrialBalanceSection(Document document) {
        document.add(new Paragraph("\nBalanza de Comprobación")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(18));

        // Agregar dos espacios entre el título y la tabla
        document.add(new Paragraph("\n"));

        // Obtener rango de fechas del período activo
        String periodRange = accountingPeriodService.getActivePeriod().getStartPeriod().toLocalDate() +
                " al " + accountingPeriodService.getActivePeriod().getEndPeriod().toLocalDate();

        // Obtener los datos de la balanza de comprobación
        TrialBalanceResponse trialBalance = trialBalanceService.getTrialBalancePdf();

        // Usar contenedores mutables para los totales
        BigDecimal[] totalInitialDebit = {BigDecimal.ZERO};
        BigDecimal[] totalInitialCredit = {BigDecimal.ZERO};
        BigDecimal[] totalPeriodDebit = {BigDecimal.ZERO};
        BigDecimal[] totalPeriodCredit = {BigDecimal.ZERO};
        BigDecimal[] totalFinalDebit = {BigDecimal.ZERO};
        BigDecimal[] totalFinalCredit = {BigDecimal.ZERO};

        // Crear una tabla con estructura adecuada: 7 columnas totales
        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2, 2, 2, 2}))
                .useAllAvailableWidth();

        // Encabezados principales
        table.addHeaderCell(createHeaderCell("Cuenta", 1, 2)); // Celda de "Cuenta"
        table.addHeaderCell(createHeaderCell("Balance Inicial", 2, 1)); // Celda fusionada de "Balance Inicial"
        table.addHeaderCell(createHeaderCell(periodRange, 2, 1));
        table.addHeaderCell(createHeaderCell("Balance Final", 2, 1)); // Celda fusionada de "Balance Final"

        // Subencabezados para cada sección
        table.addHeaderCell(createSubHeaderCell("Debe"));
        table.addHeaderCell(createSubHeaderCell("Haber"));
        table.addHeaderCell(createSubHeaderCell("Debe"));
        table.addHeaderCell(createSubHeaderCell("Haber"));
        table.addHeaderCell(createSubHeaderCell("Debe"));
        table.addHeaderCell(createSubHeaderCell("Haber"));

        // Llenar las filas de la tabla con los datos
        trialBalance.getPeriods().forEach(period -> period.getAccountBalances().forEach(account -> {
            TrialBalanceResponse.InitialBalance initialBalance = account.getInitialBalance().getFirst();
            TrialBalanceResponse.BalancePeriod balancePeriod = account.getBalancePeriod().getFirst();
            TrialBalanceResponse.FinalBalance finalBalance = account.getFinalBalance().getFirst();

            // Alinear la columna de cuentas a la izquierda
            table.addCell(createCell(account.getName()).setTextAlignment(TextAlignment.LEFT));
            table.addCell(createCell(formatCurrency(initialBalance.getDebit())));
            table.addCell(createCell(formatCurrency(initialBalance.getCredit())));
            table.addCell(createCell(formatCurrency(balancePeriod.getDebit())));
            table.addCell(createCell(formatCurrency(balancePeriod.getCredit())));
            table.addCell(createCell(formatCurrency(finalBalance.getDebit())));
            table.addCell(createCell(formatCurrency(finalBalance.getCredit())));

            // Sumar a los totales utilizando índices del arreglo
            totalInitialDebit[0] = totalInitialDebit[0].add(initialBalance.getDebit());
            totalInitialCredit[0] = totalInitialCredit[0].add(initialBalance.getCredit());
            totalPeriodDebit[0] = totalPeriodDebit[0].add(balancePeriod.getDebit());
            totalPeriodCredit[0] = totalPeriodCredit[0].add(balancePeriod.getCredit());
            totalFinalDebit[0] = totalFinalDebit[0].add(finalBalance.getDebit());
            totalFinalCredit[0] = totalFinalCredit[0].add(finalBalance.getCredit());
        }));

        // Agregar los totales al final de la tabla
        table.addFooterCell(createFooterCell("Totales"));
        table.addFooterCell(createFooterCell(formatCurrency(totalInitialDebit[0])));
        table.addFooterCell(createFooterCell(formatCurrency(totalInitialCredit[0])));
        table.addFooterCell(createFooterCell(formatCurrency(totalPeriodDebit[0])));
        table.addFooterCell(createFooterCell(formatCurrency(totalPeriodCredit[0])));
        table.addFooterCell(createFooterCell(formatCurrency(totalFinalDebit[0])));
        table.addFooterCell(createFooterCell(formatCurrency(totalFinalCredit[0])));

        // Añadir la tabla al documento
        document.add(table);
    }

    /**
     * Genera la sección del "Balance General".
     */
    private void generateBalanceSection(Document document, CompanyEntity company) throws MalformedURLException {
        // Agregar encabezado reutilizando addFinancialReportHeader con texto personalizado
        addFinancialReportHeader(document, "Balance General", false, company);

        // Definir colores
        Color headerColor = new DeviceRgb(7, 43, 84); // Azul oscuro para encabezados
        Color rowBgColor = ColorConstants.WHITE; // Blanco para filas
        Color totalColor = new DeviceRgb(240, 248, 255); // Color para totales (mismo que en balanza de comprobación)

        // Obtener datos del Balance General
        List<GeneralBalanceResponse> balances = generalBalanceService.getBalanceGeneral(accountingPeriodService.getActivePeriod().getId());

        // Variables para totales
        BigDecimal totalActivosCorrientes = BigDecimal.ZERO;
        BigDecimal totalActivosNoCorrientes = BigDecimal.ZERO;
        BigDecimal totalPasivos = BigDecimal.ZERO;
        BigDecimal totalPatrimonio = BigDecimal.ZERO;

        // Tablas para cada sección
        Table activosTable = new Table(UnitValue.createPercentArray(new float[]{3, 1})).useAllAvailableWidth();
        Table pasivoPatrimonioTable = new Table(UnitValue.createPercentArray(new float[]{3, 1})).useAllAvailableWidth();

        // Encabezados iniciales
        addSectionTitle(activosTable, "ACTIVO", headerColor);
        addSectionTitle(pasivoPatrimonioTable, "PASIVO", headerColor);

        // Clasificar los datos por categorías
        for (GeneralBalanceResponse balance : balances) {
            String category = balance.getCategory();
            String accountName = balance.getAccountName();
            BigDecimal balanceAmount = balance.getBalance();

            switch (category) {
                case "ACTIVO":
                    totalActivosCorrientes = totalActivosCorrientes.add(balanceAmount);
                    addTableRow(activosTable, accountName, balanceAmount, rowBgColor);
                    break;
                case "ACTIVO_NO_CORRIENTE":
                    totalActivosNoCorrientes = totalActivosNoCorrientes.add(balanceAmount);
                    addTableRow(activosTable, accountName, balanceAmount, rowBgColor);
                    break;
                case "PASIVO":
                    totalPasivos = totalPasivos.add(balanceAmount);
                    addTableRow(pasivoPatrimonioTable, accountName, balanceAmount, rowBgColor);
                    break;
                case "PATRIMONIO":
                    totalPatrimonio = totalPatrimonio.add(balanceAmount);
                    break;
                default:
                    // Manejar categorías desconocidas si es necesario
            }
        }

        // Totales de Activos
        addSummaryRow(activosTable, "Total Activos Corrientes", totalActivosCorrientes, totalColor);
        addSummaryRow(activosTable, "Total Activos No Corrientes", totalActivosNoCorrientes, totalColor);
        addSummaryRow(activosTable, "Total Activos", totalActivosCorrientes.add(totalActivosNoCorrientes), totalColor);

        // Totales de Pasivos
        addSummaryRow(pasivoPatrimonioTable, "Total Pasivos", totalPasivos, totalColor);

        // Espacio entre Pasivo y Patrimonio
        pasivoPatrimonioTable.addCell(new Cell(1, 2)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("")));

        // Agregar contenido de Patrimonio
        addSectionTitle(pasivoPatrimonioTable, "PATRIMONIO", headerColor);
        for (GeneralBalanceResponse balance : balances) {
            if ("PATRIMONIO".equals(balance.getCategory())) {
                addTableRow(pasivoPatrimonioTable, balance.getAccountName(), balance.getBalance(), rowBgColor);
            }
        }

        // Total Patrimonio
        addSummaryRow(pasivoPatrimonioTable, "Total Patrimonio", totalPatrimonio, totalColor);

        // Suma Total (Pasivo + Patrimonio)
        addSummaryRow(pasivoPatrimonioTable, "Total Pasivo + Patrimonio", totalPasivos.add(totalPatrimonio), totalColor);

        // Estructura final: Activos a la izquierda, Pasivo y Patrimonio a la derecha
        Table layoutTable = new Table(UnitValue.createPercentArray(new float[]{1, 0.02f, 1})).useAllAvailableWidth();

        layoutTable.addCell(new Cell().add(activosTable).setBorder(Border.NO_BORDER).setPaddingRight(20));
        layoutTable.addCell(new Cell().setBorder(Border.NO_BORDER).add(new Paragraph("")));
        layoutTable.addCell(new Cell().add(pasivoPatrimonioTable).setBorder(Border.NO_BORDER).setPaddingLeft(20));

        // Agregar la tabla al documento
        document.add(layoutTable);
    }

    private void generateIncomeStatementSection(Document document, CompanyEntity company) throws MalformedURLException {
        addFinancialReportHeader(document, "Estado de Resultados", false, company);

        Color headerColor = new DeviceRgb(7, 43, 84);
        Color totalColor = new DeviceRgb(240, 248, 255);

        List<IncomeStatementResponse> incomeStatement = incomeStatementService.getIncomeStatement(
                accountingPeriodService.getActivePeriod().getId()
        );

        Table incomeStatementTable = new Table(UnitValue.createPercentArray(new float[]{4, 2})).useAllAvailableWidth();
        setupTableHeader(incomeStatementTable, headerColor);

        IncomeStatementCalculator calculator = new IncomeStatementCalculator();
        IncomeStatementFormatter formatter = new IncomeStatementFormatter();

        // === Agrupaciones y totales base ===
        Map<String, List<IncomeStatementResponse>> groupedAccounts = calculator.groupAccountsByParent(incomeStatement);

// Ventas y deducciones sobre ventas
        formatter.addGroupHeader(incomeStatementTable, "VENTAS");
        formatter.addDetailRows(incomeStatementTable, groupedAccounts.get("VENTAS"));
        BigDecimal totalVentas = calculator.calculateTotalForGroup(groupedAccounts.get("VENTAS"));
        formatter.addSummaryRow(incomeStatementTable, "TOTAL VENTAS", totalVentas, totalColor);

        formatter.addSubGroupHeader(incomeStatementTable, "(-) Devoluciones sobre Ventas");
        formatter.addDetailRows(incomeStatementTable, groupedAccounts.get("DEVOLUCIONES SOBRE VENTAS"));
        BigDecimal devolVentas = calculator.calculateTotalForGroup(groupedAccounts.get("DEVOLUCIONES SOBRE VENTAS"));

        formatter.addSubGroupHeader(incomeStatementTable, "(-) Descuentos sobre Ventas");
        formatter.addDetailRows(incomeStatementTable, groupedAccounts.get("DESCUENTOS SOBRE VENTAS"));
        BigDecimal descVentas = calculator.calculateTotalForGroup(groupedAccounts.get("DESCUENTOS SOBRE VENTAS"));

// Ventas Netas
        BigDecimal ventasNetas = totalVentas
                .subtract(nullSafe(devolVentas))
                .subtract(nullSafe(descVentas));
        formatter.addSummaryRow(incomeStatementTable, "VENTAS NETAS", ventasNetas, totalColor);

// Compras (Costo de ventas base)
        formatter.addGroupHeader(incomeStatementTable, "COMPRAS");
        formatter.addDetailRows(incomeStatementTable, groupedAccounts.get("COMPRAS"));
        BigDecimal totalCompras = calculator.calculateTotalForGroup(groupedAccounts.get("COMPRAS"));

        BigDecimal totalImportaciones = BigDecimal.ZERO;
        if (groupedAccounts.containsKey("IMPORTACIONES")) {
            formatter.addGroupHeader(incomeStatementTable, "IMPORTACIONES");
            formatter.addDetailRows(incomeStatementTable, groupedAccounts.get("IMPORTACIONES"));
            totalImportaciones = calculator.calculateTotalForGroup(groupedAccounts.get("IMPORTACIONES"));
        }

        // Si tienes inventarios, úsalos; si no, costo = compras + importaciones
        BigDecimal inventarioInicial = calculator.calculateTotalForGroup(groupedAccounts.get("INVENTARIO INICIAL"));
        BigDecimal inventarioFinal = calculator.calculateTotalForGroup(groupedAccounts.get("INVENTARIO FINAL"));

        BigDecimal comprasTotales = nullSafe(totalCompras).add(nullSafe(totalImportaciones));

        BigDecimal costoDeVentas;
        if (inventarioInicial != null && inventarioFinal != null) {
            // Costo de Ventas = Inv. Inicial + Compras Totales – Inv. Final
            costoDeVentas = nullSafe(inventarioInicial).add(comprasTotales).subtract(nullSafe(inventarioFinal));
        } else {
            // Sin inventarios explícitos, usar Compras Totales como costo
            costoDeVentas = comprasTotales;
        }
        formatter.addSummaryRow(incomeStatementTable, "COSTO DE VENTAS", costoDeVentas, totalColor);

        // Utilidad Bruta
        BigDecimal utilidadBruta = nullSafe(ventasNetas).subtract(nullSafe(costoDeVentas));
        formatter.addSummaryRow(incomeStatementTable, "UTILIDAD BRUTA", utilidadBruta, totalColor);

        // Gastos de Operación (Admin, Generales, Depreciación)
        formatter.addGroupHeader(incomeStatementTable, "GASTOS ADMINISTRATIVOS");
        formatter.addDetailRows(incomeStatementTable, groupedAccounts.get("GASTOS ADMINISTRATIVOS"));
        BigDecimal gAdmin = calculator.calculateTotalForGroup(groupedAccounts.get("GASTOS ADMINISTRATIVOS"));
        formatter.addSummaryRow(incomeStatementTable, "TOTAL GASTOS ADMINISTRATIVOS", nullSafe(gAdmin), totalColor);

        formatter.addGroupHeader(incomeStatementTable, "GASTOS GENERALES");
        formatter.addDetailRows(incomeStatementTable, groupedAccounts.get("GASTOS GENERALES"));
        BigDecimal gGenerales = calculator.calculateTotalForGroup(groupedAccounts.get("GASTOS GENERALES"));
        formatter.addSummaryRow(incomeStatementTable, "TOTAL GASTOS GENERALES", nullSafe(gGenerales), totalColor);

        formatter.addGroupHeader(incomeStatementTable, "GASTOS POR DEPRECIACION");
        formatter.addDetailRows(incomeStatementTable, groupedAccounts.get("GASTOS POR DEPRECIACION"));
        BigDecimal gDep = calculator.calculateTotalForGroup(groupedAccounts.get("GASTOS POR DEPRECIACION"));
        formatter.addSummaryRow(incomeStatementTable, "TOTAL GASTOS POR DEPRECIACION", nullSafe(gDep), totalColor);

        BigDecimal gastosOperativos = nullSafe(gAdmin).add(nullSafe(gGenerales)).add(nullSafe(gDep));
        formatter.addSummaryRow(incomeStatementTable, "TOTAL GASTOS OPERATIVOS", gastosOperativos, totalColor);

        // Resultado Operativo
        BigDecimal resultadoOperativo = nullSafe(utilidadBruta).subtract(nullSafe(gastosOperativos));
        formatter.addSummaryRow(incomeStatementTable, "RESULTADO OPERATIVO", resultadoOperativo, totalColor);

        // Gastos Financieros (debajo del resultado operativo)
        formatter.addGroupHeader(incomeStatementTable, "GASTOS FINANCIEROS");
        formatter.addDetailRows(incomeStatementTable, groupedAccounts.get("GASTOS FINANCIEROS"));
        BigDecimal gFin = calculator.calculateTotalForGroup(groupedAccounts.get("GASTOS FINANCIEROS"));
        formatter.addSummaryRow(incomeStatementTable, "TOTAL GASTOS FINANCIEROS", nullSafe(gFin), totalColor);

        // Utilidad antes de impuestos
        BigDecimal utilidadAntesImpuestos = nullSafe(resultadoOperativo).subtract(nullSafe(gFin));
        formatter.addSummaryRow(incomeStatementTable, "UTILIDAD ANTES DE IMPUESTOS", utilidadAntesImpuestos, totalColor);

        // Impuesto sobre la Renta (se RESTA)
        BigDecimal taxRate = taxSettingsService.getTaxRateForUtility(utilidadAntesImpuestos, "Renta Gravable Mensual");
        BigDecimal incomeTax = nullSafe(utilidadAntesImpuestos).multiply(nullSafe(taxRate));
        String taxRateString = nullSafe(taxRate).multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%";
        formatter.addSummaryRow(incomeStatementTable, "Impuesto Sobre la Renta (" + taxRateString + ")", incomeTax, null);

        // Utilidad Neta
        BigDecimal utilidadNeta = nullSafe(utilidadAntesImpuestos).add(nullSafe(incomeTax));
        formatter.addSummaryRow(incomeStatementTable, "UTILIDAD O PÉRDIDA NETA DEL EJERCICIO", utilidadNeta, totalColor);


        document.add(incomeStatementTable);
    }

    private static BigDecimal nullSafe(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private void setupTableHeader(Table table, Color headerColor) {
        table.addCell(new Cell()
                .add(new Paragraph("Descripción").setBold().setFontSize(12)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(headerColor)
                .setFontColor(ColorConstants.WHITE));

        table.addCell(new Cell()
                .add(new Paragraph("Monto").setBold().setFontSize(12)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(headerColor)
                .setFontColor(ColorConstants.WHITE));
    }

    private void addSectionTitle(Table table, String title, Color headerColor) {
        table.addCell(new Cell(1, 2).add(new Paragraph(title)
                        .setBold().setFontSize(14).setFontColor(ColorConstants.WHITE))
                .setTextAlignment(TextAlignment.CENTER)
                .setBackgroundColor(headerColor));
    }

    private void addTableRow(Table table, String name, BigDecimal amount, Color rowBgColor) {
        table.addCell(new Cell().add(new Paragraph(name)).setBackgroundColor(rowBgColor));
        table.addCell(new Cell().add(new Paragraph(formatCurrency(amount))).setTextAlignment(TextAlignment.RIGHT).setBackgroundColor(rowBgColor)); // Reducir ancho
    }

    /**
     * Agrega una fila de resumen al final de las tablas (totales) con el estilo requerido.
     */
    private void addSummaryRow(Table table, String label, BigDecimal total, Color headerColor) {
        // Cambiar el color de la letra según el diseño de la balanza de comprobación
        Color totalFontColor = new DeviceRgb(0, 0, 0); // COLOR NEGRO como en la balanza de comprobación (puedes cambiarlo si es distinto)

        table.addCell(new Cell().add(new Paragraph(label).setBold())
                .setBackgroundColor(headerColor) // Color de fondo
                .setFontColor(totalFontColor)); // Actualización: Ahora usa el color correcto

        table.addCell(new Cell().add(new Paragraph(formatCurrency(total)).setBold())
                .setBackgroundColor(headerColor) // Color de fondo
                .setFontColor(totalFontColor) // Actualización: Ahora usa el color correcto
                .setTextAlignment(TextAlignment.RIGHT)); // Alineación a la derecha
    }


    /**
     * Genera una celda de encabezado con estilo general.
     */
    private Cell createHeaderCell(String content, int colSpan, int rowSpan) {
        return new Cell(rowSpan, colSpan)
                .add(new Paragraph(content).setBold().setFontColor(ColorConstants.WHITE))
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(new DeviceRgb(7, 43, 84));
    }

    /**
     * Crea celdas estilizadas de pie de página.
     */
    private Cell createFooterCell(String content) {
        return new Cell()
                .add(new Paragraph(content).setBold())
                .setTextAlignment(TextAlignment.RIGHT)
                .setBackgroundColor(new DeviceRgb(240, 248, 255));
    }

    /**
     * Formatea un valor BigDecimal en moneda local.
     */
    private String formatCurrency(BigDecimal amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("es", "HN"));
        return formatter.format(amount != null ? amount : BigDecimal.ZERO);
    }

    private Cell createCell(String content) {
        return new Cell()
                .add(new Paragraph(content))
                .setTextAlignment(TextAlignment.RIGHT);
    }

    private Cell createSubHeaderCell(String content) {
        return new Cell()
                .add(new Paragraph(content).setBold())
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBackgroundColor(new DeviceRgb(173, 216, 230));
    }

    //CIERRE ANUAL
    public void generateAnnualReportPdf(OutputStream outputStream, List<AccountingPeriodEntity> yearPeriods, CompanyEntity company) throws MalformedURLException {
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
        Document document = new Document(pdfDoc);

        // Agregar encabezado
        addFinancialReportHeader(document, "Resumen Anual", true, company);

        // Generar la sección de Balanza de Comprobación
        generateTrialBalanceSectionAnnual(document);

        // Generar la sección de Balance General
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        generateBalanceSectionAnnual(document, company);

        // Generar la sección de Estado de Resultados
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        generateIncomeStatementSectionAnnual(document, company);

        document.close();
    }

    private void generateTrialBalanceSectionAnnual(Document document) {
        document.add(new Paragraph("\nBalanza de Comprobación")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(18));

        // Agregar dos espacios entre el título y la tabla
        document.add(new Paragraph("\n"));

        // Obtener rango de fechas del período activo
        String periodRange = accountingPeriodService.getAnnualPeriod().getStartPeriod().toLocalDate() +
                " al " + accountingPeriodService.getAnnualPeriod().getEndPeriod().toLocalDate();

        // Obtener los datos de la balanza de comprobación para el año completo
        TrialBalanceResponse trialBalance = trialBalanceService.getAllTrialBalances();

        // Usar contenedores mutables para los totales
        BigDecimal[] totalInitialDebit = {BigDecimal.ZERO};
        BigDecimal[] totalInitialCredit = {BigDecimal.ZERO};
        BigDecimal[] totalPeriodDebit = {BigDecimal.ZERO};
        BigDecimal[] totalPeriodCredit = {BigDecimal.ZERO};
        BigDecimal[] totalFinalDebit = {BigDecimal.ZERO};
        BigDecimal[] totalFinalCredit = {BigDecimal.ZERO};

        // Crear una tabla con estructura adecuada: 7 columnas totales
        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2, 2, 2, 2}))
                .useAllAvailableWidth();

        // Encabezados principales
        table.addHeaderCell(createHeaderCell("Cuenta", 1, 2)); // Celda de "Cuenta"
        table.addHeaderCell(createHeaderCell("Balance Inicial", 2, 1)); // Celda fusionada de "Balance Inicial"
        table.addHeaderCell(createHeaderCell(periodRange, 2, 1));
        table.addHeaderCell(createHeaderCell("Balance Final", 2, 1)); // Celda fusionada de "Balance Final"

        // Subencabezados para cada sección
        table.addHeaderCell(createSubHeaderCell("Debe"));
        table.addHeaderCell(createSubHeaderCell("Haber"));
        table.addHeaderCell(createSubHeaderCell("Debe"));
        table.addHeaderCell(createSubHeaderCell("Haber"));
        table.addHeaderCell(createSubHeaderCell("Debe"));
        table.addHeaderCell(createSubHeaderCell("Haber"));

        // Llenar las filas de la tabla con los datos
        trialBalance.getPeriods().forEach(period -> period.getAccountBalances().forEach(account -> {
            TrialBalanceResponse.InitialBalance initialBalance = account.getInitialBalance().getFirst();
            TrialBalanceResponse.BalancePeriod balancePeriod = account.getBalancePeriod().getFirst();
            TrialBalanceResponse.FinalBalance finalBalance = account.getFinalBalance().getFirst();

            // Alinear la columna de cuentas a la izquierda
            table.addCell(createCell(account.getName()).setTextAlignment(TextAlignment.LEFT));
            table.addCell(createCell(formatCurrency(initialBalance.getDebit())));
            table.addCell(createCell(formatCurrency(initialBalance.getCredit())));
            table.addCell(createCell(formatCurrency(balancePeriod.getDebit())));
            table.addCell(createCell(formatCurrency(balancePeriod.getCredit())));
            table.addCell(createCell(formatCurrency(finalBalance.getDebit())));
            table.addCell(createCell(formatCurrency(finalBalance.getCredit())));

            // Sumar a los totales utilizando índices del arreglo
            totalInitialDebit[0] = totalInitialDebit[0].add(initialBalance.getDebit());
            totalInitialCredit[0] = totalInitialCredit[0].add(initialBalance.getCredit());
            totalPeriodDebit[0] = totalPeriodDebit[0].add(balancePeriod.getDebit());
            totalPeriodCredit[0] = totalPeriodCredit[0].add(balancePeriod.getCredit());
            totalFinalDebit[0] = totalFinalDebit[0].add(finalBalance.getDebit());
            totalFinalCredit[0] = totalFinalCredit[0].add(finalBalance.getCredit());
        }));

        // Agregar los totales al final de la tabla
        table.addFooterCell(createFooterCell("Totales"));
        table.addFooterCell(createFooterCell(formatCurrency(totalInitialDebit[0])));
        table.addFooterCell(createFooterCell(formatCurrency(totalInitialCredit[0])));
        table.addFooterCell(createFooterCell(formatCurrency(totalPeriodDebit[0])));
        table.addFooterCell(createFooterCell(formatCurrency(totalPeriodCredit[0])));
        table.addFooterCell(createFooterCell(formatCurrency(totalFinalDebit[0])));
        table.addFooterCell(createFooterCell(formatCurrency(totalFinalCredit[0])));

        // Añadir la tabla al documento
        document.add(table);
    }

    private void generateBalanceSectionAnnual(Document document, CompanyEntity company) throws MalformedURLException {
        // Agregar encabezado reutilizando addFinancialReportHeader con texto personalizado
        addFinancialReportHeader(document, "Balance General", true, company);

        // Obtener datos del Balance General para el año completo
        List<GeneralBalanceResponse> balances = generalBalanceService.getBalanceGeneral(accountingPeriodService.getAnnualPeriod().getId());

        // Definir colores
        Color headerColor = new DeviceRgb(7, 43, 84); // Azul oscuro para encabezados
        Color rowBgColor = ColorConstants.WHITE; // Blanco para filas
        Color totalColor = new DeviceRgb(240, 248, 255); // Color para totales

        // Variables para totales
        BigDecimal totalActivosCorrientes = BigDecimal.ZERO;
        BigDecimal totalActivosNoCorrientes = BigDecimal.ZERO;
        BigDecimal totalPasivos = BigDecimal.ZERO;
        BigDecimal totalPatrimonio = BigDecimal.ZERO;

        // Tablas para cada sección
        Table activosTable = new Table(UnitValue.createPercentArray(new float[]{3, 1})).useAllAvailableWidth();
        Table pasivoPatrimonioTable = new Table(UnitValue.createPercentArray(new float[]{3, 1})).useAllAvailableWidth();

        // Encabezados iniciales
        addSectionTitle(activosTable, "ACTIVO", headerColor);
        addSectionTitle(pasivoPatrimonioTable, "PASIVO", headerColor);

        // Clasificar los datos por categorías
        for (GeneralBalanceResponse balance : balances) {
            String category = balance.getCategory();
            String accountName = balance.getAccountName();
            BigDecimal balanceAmount = balance.getBalance();

            switch (category) {
                case "ACTIVO":
                    totalActivosCorrientes = totalActivosCorrientes.add(balanceAmount);
                    addTableRow(activosTable, accountName, balanceAmount, rowBgColor);
                    break;
                case "ACTIVO NO CORRIENTE":
                    totalActivosNoCorrientes = totalActivosNoCorrientes.add(balanceAmount);
                    addTableRow(activosTable, accountName, balanceAmount, rowBgColor);
                    break;
                case "PASIVO":
                    totalPasivos = totalPasivos.add(balanceAmount);
                    addTableRow(pasivoPatrimonioTable, accountName, balanceAmount, rowBgColor);
                    break;
                case "PATRIMONIO":
                    totalPatrimonio = totalPatrimonio.add(balanceAmount);
                    break;
                default:

            }
        }

        // Totales de Activos
        addSummaryRow(activosTable, "Total Activos Corrientes", totalActivosCorrientes, totalColor);
        addSummaryRow(activosTable, "Total Activos No Corrientes", totalActivosNoCorrientes, totalColor);
        addSummaryRow(activosTable, "Total Activos", totalActivosCorrientes.add(totalActivosNoCorrientes), totalColor);

        // Totales de Pasivos
        addSummaryRow(pasivoPatrimonioTable, "Total Pasivos", totalPasivos, totalColor);

        // Espacio entre Pasivo y Patrimonio
        pasivoPatrimonioTable.addCell(new Cell(1, 2)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("")));

        // Agregar contenido de Patrimonio
        addSectionTitle(pasivoPatrimonioTable, "PATRIMONIO", headerColor);
        for (GeneralBalanceResponse balance : balances) {
            if ("PATRIMONIO".equals(balance.getCategory())) {
                addTableRow(pasivoPatrimonioTable, balance.getAccountName(), balance.getBalance(), rowBgColor);
            }
        }

        // Total Patrimonio
        addSummaryRow(pasivoPatrimonioTable, "Total Patrimonio", totalPatrimonio, totalColor);

        // Suma Total (Pasivo + Patrimonio)
        addSummaryRow(pasivoPatrimonioTable, "Total Pasivo + Patrimonio", totalPasivos.add(totalPatrimonio), totalColor);

        // Estructura final: Activos a la izquierda, Pasivo y Patrimonio a la derecha
        Table layoutTable = new Table(UnitValue.createPercentArray(new float[]{1, 0.02f, 1})).useAllAvailableWidth();

        layoutTable.addCell(new Cell().add(activosTable).setBorder(Border.NO_BORDER).setPaddingRight(20));
        layoutTable.addCell(new Cell().setBorder(Border.NO_BORDER).add(new Paragraph("")));
        layoutTable.addCell(new Cell().add(pasivoPatrimonioTable).setBorder(Border.NO_BORDER).setPaddingLeft(20));

        // Agregar la tabla al documento
        document.add(layoutTable);
    }

    private void generateIncomeStatementSectionAnnual(Document document, CompanyEntity company) throws MalformedURLException {
        addFinancialReportHeader(document, "Estado de Resultados", true, company);

        Color headerColor = new DeviceRgb(7, 43, 84);
        Color totalColor = new DeviceRgb(240, 248, 255);

        // Obtener todos los registros del estado de resultados anual
        List<IncomeStatementResponse> incomeStatement = incomeStatementService.getIncomeStatement(
                accountingPeriodService.getAnnualPeriod().getId()
        );

        Table incomeStatementTable = new Table(UnitValue.createPercentArray(new float[]{4, 2})).useAllAvailableWidth();
        setupTableHeader(incomeStatementTable, headerColor);

        IncomeStatementCalculator calculator = new IncomeStatementCalculator();
        IncomeStatementFormatter formatter = new IncomeStatementFormatter();

        Map<String, List<IncomeStatementResponse>> groupedAccounts = calculator.groupAccountsByParent(incomeStatement);

        // === 1. Ventas y deducciones ===
        formatter.addGroupHeader(incomeStatementTable, "VENTAS");
        formatter.addDetailRows(incomeStatementTable, groupedAccounts.get("VENTAS"));
        BigDecimal totalVentas = calculator.calculateTotalForGroup(groupedAccounts.get("VENTAS"));
        formatter.addSummaryRow(incomeStatementTable, "TOTAL VENTAS", totalVentas, totalColor);

        formatter.addSubGroupHeader(incomeStatementTable, "(-) Devoluciones sobre Ventas");
        formatter.addDetailRows(incomeStatementTable, groupedAccounts.get("DEVOLUCIONES SOBRE VENTAS"));
        BigDecimal devolVentas = calculator.calculateTotalForGroup(groupedAccounts.get("DEVOLUCIONES SOBRE VENTAS"));

        formatter.addSubGroupHeader(incomeStatementTable, "(-) Descuentos sobre Ventas");
        formatter.addDetailRows(incomeStatementTable, groupedAccounts.get("DESCUENTOS SOBRE VENTAS"));
        BigDecimal descVentas = calculator.calculateTotalForGroup(groupedAccounts.get("DESCUENTOS SOBRE VENTAS"));

        BigDecimal ventasNetas = nullSafe(totalVentas)
                .subtract(nullSafe(devolVentas))
                .subtract(nullSafe(descVentas));
        formatter.addSummaryRow(incomeStatementTable, "VENTAS NETAS", ventasNetas, totalColor);

        // === 2. Compras y costo de ventas ===
        formatter.addGroupHeader(incomeStatementTable, "COMPRAS");
        formatter.addDetailRows(incomeStatementTable, groupedAccounts.get("COMPRAS"));
        BigDecimal totalCompras = calculator.calculateTotalForGroup(groupedAccounts.get("COMPRAS"));

        BigDecimal totalImportaciones = BigDecimal.ZERO;
        if (groupedAccounts.containsKey("IMPORTACIONES")) {
            formatter.addGroupHeader(incomeStatementTable, "IMPORTACIONES");
            formatter.addDetailRows(incomeStatementTable, groupedAccounts.get("IMPORTACIONES"));
            totalImportaciones = calculator.calculateTotalForGroup(groupedAccounts.get("IMPORTACIONES"));
        }

        BigDecimal inventarioInicial = calculator.calculateTotalForGroup(groupedAccounts.get("INVENTARIO INICIAL"));
        BigDecimal inventarioFinal = calculator.calculateTotalForGroup(groupedAccounts.get("INVENTARIO FINAL"));

        BigDecimal comprasTotales = nullSafe(totalCompras).add(nullSafe(totalImportaciones));

        BigDecimal costoDeVentas;
        if (inventarioInicial != null && inventarioFinal != null) {
            costoDeVentas = nullSafe(inventarioInicial)
                    .add(comprasTotales)
                    .subtract(nullSafe(inventarioFinal));
        } else {
            costoDeVentas = comprasTotales;
        }
        formatter.addSummaryRow(incomeStatementTable, "COSTO DE VENTAS", costoDeVentas, totalColor);

        // === 3. Utilidad Bruta ===
        BigDecimal utilidadBruta = nullSafe(ventasNetas).subtract(nullSafe(costoDeVentas));
        formatter.addSummaryRow(incomeStatementTable, "UTILIDAD BRUTA", utilidadBruta, totalColor);

        // === 4. Gastos de operación ===
        formatter.addGroupHeader(incomeStatementTable, "GASTOS ADMINISTRATIVOS");
        formatter.addDetailRows(incomeStatementTable, groupedAccounts.get("GASTOS ADMINISTRATIVOS"));
        BigDecimal gAdmin = calculator.calculateTotalForGroup(groupedAccounts.get("GASTOS ADMINISTRATIVOS"));
        formatter.addSummaryRow(incomeStatementTable, "TOTAL GASTOS ADMINISTRATIVOS", nullSafe(gAdmin), totalColor);

        formatter.addGroupHeader(incomeStatementTable, "GASTOS GENERALES");
        formatter.addDetailRows(incomeStatementTable, groupedAccounts.get("GASTOS GENERALES"));
        BigDecimal gGenerales = calculator.calculateTotalForGroup(groupedAccounts.get("GASTOS GENERALES"));
        formatter.addSummaryRow(incomeStatementTable, "TOTAL GASTOS GENERALES", nullSafe(gGenerales), totalColor);

        formatter.addGroupHeader(incomeStatementTable, "GASTOS POR DEPRECIACION");
        formatter.addDetailRows(incomeStatementTable, groupedAccounts.get("GASTOS POR DEPRECIACION"));
        BigDecimal gDep = calculator.calculateTotalForGroup(groupedAccounts.get("GASTOS POR DEPRECIACION"));
        formatter.addSummaryRow(incomeStatementTable, "TOTAL GASTOS POR DEPRECIACION", nullSafe(gDep), totalColor);

        BigDecimal gastosOperativos = nullSafe(gAdmin)
                .add(nullSafe(gGenerales))
                .add(nullSafe(gDep));
        formatter.addSummaryRow(incomeStatementTable, "TOTAL GASTOS OPERATIVOS", gastosOperativos, totalColor);

        // === 5. Resultado Operativo ===
        BigDecimal resultadoOperativo = nullSafe(utilidadBruta).subtract(nullSafe(gastosOperativos));
        formatter.addSummaryRow(incomeStatementTable, "RESULTADO OPERATIVO", resultadoOperativo, totalColor);

        // === 6. Gastos Financieros ===
        formatter.addGroupHeader(incomeStatementTable, "GASTOS FINANCIEROS");
        formatter.addDetailRows(incomeStatementTable, groupedAccounts.get("GASTOS FINANCIEROS"));
        BigDecimal gFin = calculator.calculateTotalForGroup(groupedAccounts.get("GASTOS FINANCIEROS"));
        formatter.addSummaryRow(incomeStatementTable, "TOTAL GASTOS FINANCIEROS", nullSafe(gFin), totalColor);

        // === 7. Utilidad Antes de Impuestos ===
        BigDecimal utilidadAntesImpuestosAnual = nullSafe(resultadoOperativo).subtract(nullSafe(gFin));
        formatter.addSummaryRow(incomeStatementTable, "UTILIDAD ANTES DE IMPUESTOS", utilidadAntesImpuestosAnual, totalColor);

        // === 8. Impuesto sobre la Renta (RESTADO) ===
        BigDecimal taxRateAnnual = taxSettingsService.getTaxRateForUtility(utilidadAntesImpuestosAnual, "Renta Gravable Anual");
        BigDecimal incomeTaxAnnual = nullSafe(utilidadAntesImpuestosAnual).multiply(nullSafe(taxRateAnnual));

        String taxRateString = nullSafe(taxRateAnnual)
                .multiply(BigDecimal.valueOf(100))
                .stripTrailingZeros()
                .toPlainString() + "%";
        formatter.addSummaryRow(incomeStatementTable, "Impuesto Sobre la Renta (" + taxRateString + ")", incomeTaxAnnual, null);

        // === 9. Utilidad Neta del Ejercicio ===
        BigDecimal utilidadNetaAnual = nullSafe(utilidadAntesImpuestosAnual).add(nullSafe(incomeTaxAnnual));
        formatter.addSummaryRow(incomeStatementTable, "UTILIDAD O PÉRDIDA NETA DEL EJERCICIO", utilidadNetaAnual, totalColor);

        document.add(incomeStatementTable);
    }

}