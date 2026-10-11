package com.saurabh.financemanager.category;

import com.saurabh.financemanager.transaction.TransactionType;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class BuiltInCategoryInitializer implements ApplicationRunner {
    public static final String UNCATEGORIZED_EXPENSE = "UNCATEGORIZED_EXPENSE";
    public static final String UNCATEGORIZED_INCOME = "UNCATEGORIZED_INCOME";

    private record BuiltIn(String code, String name, TransactionType type, String keywords){
    }

    private static final List<BuiltIn> BUILT_INS=List.of(
            new BuiltIn("GROCERIES", "Groceries", TransactionType.EXPENSE,
                                "rewe,lidl,aldi,edeka,netto,penny,kaufland,tegut"),
            new BuiltIn("EATING_OUT", "Eating Out", TransactionType.EXPENSE,
                    "restaurant,mcdonald,burger king,kfc,lieferando,pizza"),
            new BuiltIn("TRANSPORT", "Transport", TransactionType.EXPENSE,
                    "deutsche bahn,rmv,tankstelle,aral,flixbus,uber"),
            new BuiltIn("RENT", "Rent", TransactionType.EXPENSE,
                    "miete,kaltmiete"),
            new BuiltIn("UTILITIES", "Utilities", TransactionType.EXPENSE,
                    "strom,stadtwerke,mainova,telekom,vodafone"),
            new BuiltIn("ENTERTAINMENT", "Entertainment", TransactionType.EXPENSE,
                    "netflix,spotify,kino,disney"),
            new BuiltIn("SHOPPING", "Shopping", TransactionType.EXPENSE,
                    "amazon,zalando,ikea,mediamarkt"),
            new BuiltIn(UNCATEGORIZED_EXPENSE, "Uncategorized", TransactionType.EXPENSE, null),

            new BuiltIn("SALARY", "Salary", TransactionType.INCOME,
                    "gehalt,salary,lohn"),
            new BuiltIn("OTHER_INCOME", "Other Income", TransactionType.INCOME,
                    "erstattung,refund"),
            new BuiltIn(UNCATEGORIZED_INCOME, "Uncategorized", TransactionType.INCOME, null)
    );
    private final CategoryRepository categoryRepository;

    public BuiltInCategoryInitializer(CategoryRepository categoryRepository){
        this.categoryRepository=categoryRepository;
    }
    @Override
    @Transactional
    public void run(ApplicationArguments args){
        for (BuiltIn builtIn:BUILT_INS){
            if (categoryRepository.findByCode(builtIn.code()).isPresent()){
                continue;
            }

            Category category = categoryRepository
                    .findByNameIgnoreCaseAndTransactionType(builtIn.name(), builtIn.type())
                    .orElseGet(Category::new);

            category.setName(builtIn.name());
            category.setTransactionType(builtIn.type());
            category.setCode(builtIn.code());
            category.setSystemCategory(true);

            if (category.getKeywords() == null) {
                category.setKeywords(KeywordCleaner.clean(builtIn.keywords()));
            }
            categoryRepository.save(category);
        }
    }

}
