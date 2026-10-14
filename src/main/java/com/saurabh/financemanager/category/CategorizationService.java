package com.saurabh.financemanager.category;

import com.saurabh.financemanager.transaction.TransactionType;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class CategorizationService {

    private final CategoryRepository categoryRepository;

    public CategorizationService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public Optional<Category> categorize(UUID userId, String description, TransactionType type) {
        if (description == null || description.isBlank() || type == null) {
            return Optional.empty();
        }

        String text = description.toLowerCase();
        Category bestCategory = null;
        String bestKeyword = null;

        for (Category category : categoryRepository.findByTransactionType(type)) {
            if (category.getKeywords() == null) {
                continue;
            }
            for (String keyword : category.getKeywords().split(",")) {
                boolean longer = bestKeyword == null || keyword.length() > bestKeyword.length();
                if (text.contains(keyword) && longer) {
                    bestKeyword = keyword;
                    bestCategory = category;
                }
            }
        }

        return Optional.ofNullable(bestCategory);
    }
}