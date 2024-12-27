package com.sti.accounting.utils;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.sti.accounting.models.IncomeStatementResponse;

import java.math.BigDecimal;
import java.util.List;

public class IncomeStatementFormatter {

    public void addGroupHeader(Table table, String headerText) {
        Cell headerCell = new Cell(1, 2)
                .add(new Paragraph(headerText).setBold().setFontSize(11))
                .setTextAlignment(TextAlignment.LEFT)
                .setPadding(5);
        table.addCell(headerCell);
    }

    public void addSubGroupHeader(Table table, String headerText) {
        Cell headerCell = new Cell(1, 2)
                .add(new Paragraph(headerText).setItalic().setFontSize(10))
                .setTextAlignment(TextAlignment.LEFT)
                .setPaddingLeft(15);
        table.addCell(headerCell);
    }

    public void addDetailRows(Table table, List<IncomeStatementResponse> accounts) {
        if (accounts == null) return;

        accounts.forEach(account -> {
            table.addCell(new Cell()
                    .add(new Paragraph(account.getAccount()).setFontSize(9))
                    .setPaddingLeft(20));
            table.addCell(new Cell()
                    .add(new Paragraph(formatCurrency(account.getAmount())).setFontSize(9))
                    .setTextAlignment(TextAlignment.RIGHT));
        });
    }

    public void addSummaryRow(Table table, String label, BigDecimal amount, Color backgroundColor) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label).setBold().setFontSize(10));
        Cell amountCell = new Cell()
                .add(new Paragraph(formatCurrency(amount)).setBold().setFontSize(10))
                .setTextAlignment(TextAlignment.RIGHT);

        if (backgroundColor != null) {
            labelCell.setBackgroundColor(backgroundColor);
            amountCell.setBackgroundColor(backgroundColor);
        }

        table.addCell(labelCell);
        table.addCell(amountCell);
    }

    private String formatCurrency(BigDecimal amount) {
        return CurrencyFormatter.format(amount);
    }
}