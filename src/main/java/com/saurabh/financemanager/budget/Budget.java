package com.saurabh.financemanager.budget;

import com.saurabh.financemanager.category.Category;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Cleanup;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "budgets",
        uniqueConstraints = @UniqueConstraint(columnNames={"category_id","month"}))
public class Budget {
    @Id
    @GeneratedValue(strategy= GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "category_id",nullable = false)
    private Category category;

    //Always the first day of the month eg: 2026-10-01 for October
    @Column(nullable = false)
    private LocalDate month;

    @Column(nullable = false,precision = 19,scale = 2)
    private BigDecimal amountLimit;

    @CreationTimestamp
    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


}
