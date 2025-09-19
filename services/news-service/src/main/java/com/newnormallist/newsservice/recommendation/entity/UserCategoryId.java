package com.newnormallist.newsservice.recommendation.entity;

import lombok.*;
import jakarta.persistence.*;
import java.io.Serializable;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @EqualsAndHashCode
public class UserCategoryId implements Serializable {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 32)
    private RecommendationCategory category;
}
