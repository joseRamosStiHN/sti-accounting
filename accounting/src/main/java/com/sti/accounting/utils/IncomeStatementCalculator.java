package com.sti.accounting.utils;

import com.sti.accounting.models.IncomeStatementResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IncomeStatementCalculator {

    public Map<String, List<IncomeStatementResponse>> groupAccountsByParent(List<IncomeStatementResponse> accounts) {
        return accounts.stream()
                .filter(item -> item.getAccountParent() != null)
                .collect(Collectors.groupingBy(IncomeStatementResponse::getAccountParent));
    }

    public BigDecimal calculateTotalForGroup(List<IncomeStatementResponse> accounts) {
        if (accounts == null) return BigDecimal.ZERO;
        return accounts.stream()
                .map(IncomeStatementResponse::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateNetSales(Map<String, List<IncomeStatementResponse>> groupedAccounts) {
        BigDecimal ventas = calculateTotalForGroup(groupedAccounts.get("VENTAS"));
        BigDecimal devoluciones = calculateTotalForGroup(groupedAccounts.get("DEVOLUCIONES SOBRE VENTAS"));
        BigDecimal descuentos = calculateTotalForGroup(groupedAccounts.get("DESCUENTOS SOBRE VENTAS"));
        return ventas.subtract(devoluciones).subtract(descuentos);
    }

    public BigDecimal calculateGrossProfit(BigDecimal netSales, BigDecimal costoVentas) {
        return netSales.subtract(costoVentas);
    }

    public BigDecimal calculateOperatingExpenses(Map<String, List<IncomeStatementResponse>> groupedAccounts) {
        BigDecimal gastosAdmin = calculateTotalForGroup(groupedAccounts.get("GASTOS ADMINISTRATIVOS"));
        BigDecimal gastosFinancieros = calculateTotalForGroup(groupedAccounts.get("GASTOS FINANCIEROS"));
        BigDecimal gastosGenerales = calculateTotalForGroup(groupedAccounts.get("GASTOS GENERALES"));
        BigDecimal gastosDepreciacion = calculateTotalForGroup(groupedAccounts.get("GASTOS POR DEPRECIACION"));

        return gastosAdmin
                .add(gastosFinancieros)
                .add(gastosGenerales)
                .add(gastosDepreciacion);
    }
}