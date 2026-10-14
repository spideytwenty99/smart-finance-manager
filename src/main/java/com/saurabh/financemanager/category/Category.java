package com.saurabh.financemanager.category;


import com.saurabh.financemanager.transaction.TransactionType;
import com.saurabh.financemanager.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "categories",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "name", "transaction_type"}),
                @UniqueConstraint(columnNames = {"user_id", "code"})
        })
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType transactionType;


    // First version for automatic categorization
    @Column(length = 500)
    private String keywords;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean systemCategory;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 50)          // no longer unique on its own
    private String code;


}
