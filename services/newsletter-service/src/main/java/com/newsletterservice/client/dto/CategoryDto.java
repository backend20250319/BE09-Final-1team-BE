package com.newsletterservice.client.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoryDto {
    private String categoryCode;
    private String categoryName;
    private String icon;
}
