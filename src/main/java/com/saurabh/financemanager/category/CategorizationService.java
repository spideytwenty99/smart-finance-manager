package com.saurabh.financemanager.category;

import com.saurabh.financemanager.ml.MlClient;
import com.saurabh.financemanager.transaction.TransactionType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class CategorizationService {

    private final CategoryRepository categoryRepository;
    private final MlClient mlClient;
    private final double confidenceThreshold;

    public CategorizationService(CategoryRepository categoryRepository,
                                 MlClient mlClient,
                                 @Value("${ml.confidence-threshold}") double confidenceThreshold) {
        this.categoryRepository = categoryRepository;
        this.mlClient = mlClient;
        this.confidenceThreshold = confidenceThreshold;
    }

    public Optional<Category> categorize(UUID userId, String description, TransactionType type) {
        if (description == null || description.isBlank() || type == null) {
            return Optional.empty();
        }

        //  The user's own keyword rules always win
        Optional<Category> byKeyword = matchKeywords(userId, description, type);
        if (byKeyword.isPresent()) {
            return byKeyword;
        }

        //Otherwise ask the model, and only trust confident predictions
        return predictWithModel(userId, description, type);
    }

    private Optional<Category> matchKeywords(UUID userId, String description, TransactionType type) {
        String text = description.toLowerCase();
        Category bestCategory = null;
        String bestKeyword = null;

        for (Category category : categoryRepository.findByUserIdAndTransactionType(userId, type)) {
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

    private Optional<Category> predictWithModel(UUID userId, String description, TransactionType type) {
        return mlClient.predictCategory(description, type)
                .filter(prediction -> prediction.confidence() >= confidenceThreshold)
                .flatMap(prediction -> categoryRepository
                        .findByUserIdAndNameIgnoreCaseAndTransactionType(userId, prediction.category(), type));
    }
}