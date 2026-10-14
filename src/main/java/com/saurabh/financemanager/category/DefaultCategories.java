package com.saurabh.financemanager.category;

import com.saurabh.financemanager.transaction.TransactionType;

import java.util.List;

public final class DefaultCategories {

    public static final String UNCATEGORIZED_EXPENSE = "UNCATEGORIZED_EXPENSE";
    public static final String UNCATEGORIZED_INCOME = "UNCATEGORIZED_INCOME";

    // code is only set for the two protected Uncategorized categories
    public record DefaultCategory(String name, TransactionType type, String keywords, String code) {}

    public static final List<DefaultCategory> ALL = List.of(
            new DefaultCategory("Groceries", TransactionType.EXPENSE, "rewe,lidl,aldi,edeka,netto,penny,kaufland,tegut", null),
            new DefaultCategory("Eating Out", TransactionType.EXPENSE, "restaurant,mcdonald,burger king,kfc,lieferando,pizza", null),
            new DefaultCategory("Transport", TransactionType.EXPENSE, "deutsche bahn,rmv,tankstelle,aral,flixbus,uber", null),
            new DefaultCategory("Rent", TransactionType.EXPENSE, "miete,kaltmiete", null),
            new DefaultCategory("Utilities", TransactionType.EXPENSE, "strom,stadtwerke,mainova,telekom,vodafone", null),
            new DefaultCategory("Entertainment", TransactionType.EXPENSE, "kino,disney", null),
            new DefaultCategory("Subscriptions", TransactionType.EXPENSE, "netflix,spotify,amazon prime", null),
            new DefaultCategory("Shopping", TransactionType.EXPENSE, "amazon,zalando,ikea,mediamarkt", null),
            new DefaultCategory("Health", TransactionType.EXPENSE, "apotheke", null),
            new DefaultCategory("Uncategorized", TransactionType.EXPENSE, null, UNCATEGORIZED_EXPENSE),
            new DefaultCategory("Salary", TransactionType.INCOME, "gehalt,salary,lohn", null),
            new DefaultCategory("Other Income", TransactionType.INCOME, "erstattung,refund", null),
            new DefaultCategory("Uncategorized", TransactionType.INCOME, null, UNCATEGORIZED_INCOME)
    );

    private DefaultCategories() {
    }
}