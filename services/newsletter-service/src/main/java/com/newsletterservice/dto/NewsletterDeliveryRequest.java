package com.newsletterservice.dto;

import com.newsletterservice.entity.DeliveryMethod;
import lombok.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterDeliveryRequest {

    @NotNull(message = "뉴스레터 ID는 필수입니다")
    private Long newsletterId;

    private List<Long> targetUserIds;

    @Builder.Default
    private DeliveryMethod deliveryMethod = DeliveryMethod.EMAIL;

    @Builder.Default
    private boolean isPersonalized = true;

    @Builder.Default
    private boolean isScheduled = false;

    private LocalDateTime scheduledAt; // ISO format
}
