package com.newnormallist.newsservice.recommendation.entity;

import lombok.*;
import jakarta.persistence.*;

// 사용자의 선호 카테고리 -> P(c)를 만들 때 균등분포 (1/k)로 환산
@Entity
@Table(
    name = "user_categories",
    uniqueConstraints = {
        @UniqueConstraint(name = "uniq_user_category", columnNames = {"user_id", "category"})
    }
)
@Getter
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class UserCategories {

    @EmbeddedId
    private UserCategoryId id;
}