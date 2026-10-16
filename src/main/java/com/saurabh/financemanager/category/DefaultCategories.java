package com.saurabh.financemanager.category;

import com.saurabh.financemanager.transaction.TransactionType;

import java.util.List;

public final class DefaultCategories {

    public static final String UNCATEGORIZED_EXPENSE = "UNCATEGORIZED_EXPENSE";
    public static final String UNCATEGORIZED_INCOME = "UNCATEGORIZED_INCOME";

    // code is only set for the two protected Uncategorized categories
    public record DefaultCategory(String name, TransactionType type, String keywords, String code) {}

    // Keywords: only long, unique merchant names, taken from the public sources
    // listed in ml-service/training/merchants.py. Short or common names (e.g. otto, tier)
    // are left to the ML model, because a keyword matches anywhere in the text.
    private static final String GROCERIES = "rewe,lidl,aldi,edeka,netto,penny,kaufland,tegut,"
            + "alnatura,bio company,famila,marktkauf,selgros,nahkauf,carrefour,auchan,migros,denner,"
            + "waitrose,rossmann,drogerie,flaschenpost,bringmeister,gorillas,hellofresh";
    private static final String EATING_OUT = "restaurant,mcdonald,burger king,kfc,lieferando,pizza,"
            + "uber eats,hans im glueck,hans im glück,peter pane,frittenwerk,five guys,vapiano,"
            + "burgermeister,subway,dean&david,dean and david,le crobag,kamps,backwerk,baeckerei,"
            + "bäckerei,starbucks,nordsee";
    private static final String TRANSPORT = "deutsche bahn,rmv,tankstelle,aral,flixbus,uber,"
            + "flixtrain,blablacar,free now,nextbike,db vertrieb,tier mobility,share now,eurowings";
    private static final String UTILITIES = "strom,stadtwerke,mainova,telekom,vodafone,"
            + "freenet,telefonica,vattenfall,lichtblick,congstar,entega";
    private static final String ENTERTAINMENT = "kino,disney,"
            + "cinestar,cinemaxx,eventim,ticketmaster,playstation,nintendo";
    private static final String SUBSCRIPTIONS = "netflix,spotify,amazon prime,"
            + "crunchyroll,paramount,hbo max,viaplay,youtube premium,audible,microsoft 365,deezer";
    private static final String SHOPPING = "amazon,zalando,ikea,mediamarkt,"
            + "aliexpress,tchibo,galeria,bonprix,about you,breuninger,deichmann,snipes,adidas,"
            + "bestsecret,lascana,sephora,douglas,flaconi,notino,saturn,cyberport,notebooksbilliger,"
            + "mindfactory,wayfair,westwing,xxxlutz,hornbach,bauhaus,hagebau,hellweg,manomano,"
            + "thalia,hugendubel,medimops,momox,kleinanzeigen,vinted,booklooker,gamestop,"
            + "decathlon,intersport,sportscheck,bergfreunde,zooplus,fressnapf,zooroyal,"
            + "shein,ebay,temu,primark,tk maxx";
    private static final String HEALTH = "apotheke,docmorris,medpex,aponeo,fielmann,apollo optik";

    public static final List<DefaultCategory> ALL = List.of(
            new DefaultCategory("Groceries", TransactionType.EXPENSE, GROCERIES, null),
            new DefaultCategory("Eating Out", TransactionType.EXPENSE, EATING_OUT, null),
            new DefaultCategory("Transport", TransactionType.EXPENSE, TRANSPORT, null),
            new DefaultCategory("Rent", TransactionType.EXPENSE, "miete,kaltmiete", null),
            new DefaultCategory("Utilities", TransactionType.EXPENSE, UTILITIES, null),
            new DefaultCategory("Entertainment", TransactionType.EXPENSE, ENTERTAINMENT, null),
            new DefaultCategory("Subscriptions", TransactionType.EXPENSE, SUBSCRIPTIONS, null),
            new DefaultCategory("Shopping", TransactionType.EXPENSE, SHOPPING, null),
            new DefaultCategory("Health", TransactionType.EXPENSE, HEALTH, null),
            new DefaultCategory("Uncategorized", TransactionType.EXPENSE, null, UNCATEGORIZED_EXPENSE),
            new DefaultCategory("Salary", TransactionType.INCOME, "gehalt,salary,lohn", null),
            new DefaultCategory("Other Income", TransactionType.INCOME, "erstattung,refund", null),
            new DefaultCategory("Uncategorized", TransactionType.INCOME, null, UNCATEGORIZED_INCOME)
    );

    private DefaultCategories() {
    }
}