package com.sti.accounting.utils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyFormatter {
    private static final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("es", "HN"));

    public static String format(BigDecimal amount) {
        return currencyFormatter.format(amount.doubleValue());
    }
}
