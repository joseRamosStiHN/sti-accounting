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
import com.sti.accounting.models.GeneralBalanceResponse;
import com.sti.accounting.models.IncomeStatementResponse;
import com.sti.accounting.models.TrialBalanceResponse;
import com.sti.accounting.services.AccountingPeriodService;
import com.sti.accounting.services.GeneralBalanceService;
import com.sti.accounting.services.IncomeStatementService;
import com.sti.accounting.services.TrialBalanceService;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Component
public class ReportPdfGenerator {

    private final GeneralBalanceService generalBalanceService;
    private final IncomeStatementService incomeStatementService;
    private final TrialBalanceService trialBalanceService;
    private final AccountingPeriodService accountingPeriodService;

    public ReportPdfGenerator(GeneralBalanceService generalBalanceService,
                              IncomeStatementService incomeStatementService,
                              TrialBalanceService trialBalanceService,
                              AccountingPeriodService accountingPeriodService) {
        this.generalBalanceService = generalBalanceService;
        this.incomeStatementService = incomeStatementService;
        this.trialBalanceService = trialBalanceService;
        this.accountingPeriodService = accountingPeriodService;
    }

    /**
     * Método principal que genera el PDF completo con todas las secciones del reporte.
     */
    public void generateReportPdf(OutputStream outputStream) throws MalformedURLException {
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDoc = new PdfDocument(writer);
        pdfDoc.setDefaultPageSize(PageSize.A4.rotate());
        Document document = new Document(pdfDoc);

        // 1. Sección de "Reportes Financieros"
        addFinancialReportHeader(document, "Reportes Financieros");

        // 2. Sección de "Balanza de Comprobación"
        generateTrialBalanceSection(document);

        // 3. Sección de "Balance General"
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        generateBalanceSection(document);

        // 4. Sección de "Estado de Resultados"
        document.add(new AreaBreak(AreaBreakType.NEXT_PAGE));
        generateIncomeStatementSection(document);

        document.close();
    }

    /**
     * Agrega el encabezado principal reutilizable con texto personalizado.
     */
    private void addFinancialReportHeader(Document document, String reportTitle) throws MalformedURLException {
        String periodRange = accountingPeriodService.getActivePeriod().getStartPeriod().toLocalDate() +
                " al " + accountingPeriodService.getActivePeriod().getEndPeriod().toLocalDate();

        // Cargar el recurso desde el directorio src/main/resources
        String logoPath = getClass().getClassLoader().getResource("logo.jpg").getPath();
        Image logo = new Image(ImageDataFactory.create(logoPath)).setWidth(150).setHeight(70);

        // Crear un contenedor de tabla con proporciones ajustadas (balances iguales)
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1})).useAllAvailableWidth();

        // Celda para el logo (lado izquierdo)
        headerTable.addCell(new Cell().add(logo).setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER));

        // Celda para los textos (centrados)
        Cell textCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER);
        textCell.add(new Paragraph("Empresa S.A. de C.V.")
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(20));
        textCell.add(new Paragraph(reportTitle)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(16));
        textCell.add(new Paragraph("Período: " + periodRange)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFontSize(12));
        headerTable.addCell(textCell);

        // Celda vacía (lado derecho para equilibrar centrado)
        headerTable.addCell(new Cell().setBorder(Border.NO_BORDER));

        // Agregar la tabla al documento
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
        TrialBalanceResponse trialBalance = trialBalanceService.getTrialBalance();

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
    private void generateBalanceSection(Document document) throws MalformedURLException {
        // Agregar encabezado reutilizando addFinancialReportHeader con texto personalizado
        addFinancialReportHeader(document, "Balance General");

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

    private void generateIncomeStatementSection(Document document) throws MalformedURLException {
        // Usar el encabezado genérico reutilizando el método de encabezado
        addFinancialReportHeader(document, "Estado de Resultados");

        // Definir colores
        Color headerColor = new DeviceRgb(7, 43, 84); // Azul para encabezados
        Color totalColor = new DeviceRgb(240, 248, 255); // Fondo del total (igual al de la balanza de comprobación)
        Color totalFontColor = new DeviceRgb(0, 0, 0); // Color negro para el texto de totales

        // Obtener los datos del estado de resultados
        List<IncomeStatementResponse> incomeStatement = incomeStatementService.getIncomeStatement(
                accountingPeriodService.getActivePeriod().getId()
        );

        // Crear tabla para el estado de resultados
        Table incomeStatementTable = new Table(UnitValue.createPercentArray(new float[]{4, 2})).useAllAvailableWidth();

        // Encabezado de la tabla
        incomeStatementTable.addCell(new Cell().add(new Paragraph("Descripción").setBold().setFontSize(12).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(headerColor).setFontColor(ColorConstants.WHITE));
        incomeStatementTable.addCell(new Cell().add(new Paragraph("Monto").setBold().setFontSize(12).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(headerColor).setFontColor(ColorConstants.WHITE));

        // Variables para acumulados
        BigDecimal totalVentas = BigDecimal.ZERO;
        BigDecimal devoluciones = BigDecimal.ZERO;
        BigDecimal rebajas = BigDecimal.ZERO;
        BigDecimal costoVentas = BigDecimal.ZERO;
        BigDecimal gastosGenerales = BigDecimal.ZERO;
        BigDecimal otrosIngresosYGastos = BigDecimal.ZERO;
        BigDecimal resultadoFinanciero = BigDecimal.ZERO;
        BigDecimal impuesto = BigDecimal.ZERO;

        // Clasificar y agregar datos
        for (IncomeStatementResponse item : incomeStatement) {
            String category = item.getAccountParent();

            // Validar si el campo category es nulo
            if (category == null) {

                continue;
            }

            // Lógica de clasificación
            switch (category) {
                case "VENTAS":
                    totalVentas = totalVentas.add(item.getAmount());
                    break;
                case "COMPRAS":
                    devoluciones = devoluciones.add(item.getAmount());
                    break;
                case "GASTOS":
                    rebajas = rebajas.add(item.getAmount());
                    break;
                case "GASTOS ADMINISTRATIVOS":
                    costoVentas = costoVentas.add(item.getAmount());
                    break;
                case "GASTOS FINANCIEROS":
                    gastosGenerales = gastosGenerales.add(item.getAmount());
                    break;
                case "GASTOS GENERALES":
                    otrosIngresosYGastos = otrosIngresosYGastos.add(item.getAmount());
                    break;
                case "GASTOS POR DEPRECIACION":
                    resultadoFinanciero = resultadoFinanciero.add(item.getAmount());
                    break;
                case "IMPUESTO":
                    impuesto = impuesto.add(item.getAmount());
                    break;
                default:
            }
        }
        // Calcular totales
        BigDecimal ventasNetas = totalVentas.subtract(devoluciones).subtract(rebajas);
        BigDecimal utilidadBruta = ventasNetas.subtract(costoVentas);
        BigDecimal utilidadAntesImpuestos = utilidadBruta.subtract(gastosGenerales).add(otrosIngresosYGastos).add(resultadoFinanciero);
        BigDecimal utilidadNeta = utilidadAntesImpuestos.subtract(impuesto);

        // Llenar los datos en la tabla
        incomeStatementTable.addCell(new Cell().add(new Paragraph("Ventas totales")));
        incomeStatementTable.addCell(new Cell().add(new Paragraph(formatCurrency(totalVentas))));

        incomeStatementTable.addCell(new Cell().add(new Paragraph("(–) Devoluciones s/ventas")));
        incomeStatementTable.addCell(new Cell().add(new Paragraph(formatCurrency(devoluciones))));

        incomeStatementTable.addCell(new Cell().add(new Paragraph("(–) Rebajas sobre ventas")));
        incomeStatementTable.addCell(new Cell().add(new Paragraph(formatCurrency(rebajas))));

        // Aplicar diseño de totales: Ventas Netas
        addSummaryRow(incomeStatementTable, "Ventas netas", ventasNetas, totalColor);

        incomeStatementTable.addCell(new Cell().add(new Paragraph("(–) Costo de ventas")));
        incomeStatementTable.addCell(new Cell().add(new Paragraph(formatCurrency(costoVentas))));

        // Aplicar diseño de totales: Utilidad Bruta
        addSummaryRow(incomeStatementTable, "(=) Utilidad (pérdida) bruta", utilidadBruta, totalColor);

        incomeStatementTable.addCell(new Cell().add(new Paragraph("(–) Total de gastos generales")));
        incomeStatementTable.addCell(new Cell().add(new Paragraph(formatCurrency(gastosGenerales))));

        incomeStatementTable.addCell(new Cell().add(new Paragraph("(+/–) Otros ingresos y gastos")));
        incomeStatementTable.addCell(new Cell().add(new Paragraph(formatCurrency(otrosIngresosYGastos))));

        incomeStatementTable.addCell(new Cell().add(new Paragraph("(+/–) Resultado integral de financiamiento")));
        incomeStatementTable.addCell(new Cell().add(new Paragraph(formatCurrency(resultadoFinanciero))));

        incomeStatementTable.addCell(new Cell().add(new Paragraph("(+/–) Partidas no ordinarias")));
        incomeStatementTable.addCell(new Cell().add(new Paragraph(formatCurrency(BigDecimal.ZERO)))); // Ejemplo fijo 0

        // Aplicar diseño de totales: Utilidad Antes de Impuestos
        addSummaryRow(incomeStatementTable, "(=) Utilidad antes de impuestos", utilidadAntesImpuestos, totalColor);

        incomeStatementTable.addCell(new Cell().add(new Paragraph("(–) Impuesto a la utilidad (28%)")));
        incomeStatementTable.addCell(new Cell().add(new Paragraph(formatCurrency(impuesto))));

        // Aplicar diseño de totales: Utilidad Neta
        addSummaryRow(incomeStatementTable, "(=) Utilidad (pérdida) neta", utilidadNeta, totalColor);

        // Agregar la tabla al documento
        document.add(incomeStatementTable);
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
}