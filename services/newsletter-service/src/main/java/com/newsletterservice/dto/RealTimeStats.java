package com.newsletterservice.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RealTimeStats {
    private long pendingCount;
    private long processingCount;
    private long sentCount;
    private long openedCount;
    private long failedCount;
    private long bouncedCount;
    private LocalDateTime timestamp;
    private int analysisPeriodHours;
}
