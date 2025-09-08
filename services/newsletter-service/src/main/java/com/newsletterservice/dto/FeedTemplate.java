package com.newsletterservice.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

/**
 * 카카오톡 피드 템플릿 DTO
 * 피드 A형과 피드 B형을 지원합니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class FeedTemplate {
    
    /**
     * 
     * 
     * 피드 템플릿 타입 (A형 또는 B형)
     */
    public enum FeedType {
        FEED_A,  // 피드 A형: 기본 피드 템플릿
        FEED_B   // 피드 B형: 아이템이 추가된 피드 템플릿
    }
    
    private FeedType feedType;
    private Content content;
    private List<Button> buttons;
    private List<ItemContent> itemContents; // 피드 B형에서만 사용
    
    /**
     * 피드 템플릿의 메인 콘텐츠
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class Content {
        private String title;           // 메시지 제목
        private String description;     // 메시지 설명
        private String imageUrl;        // 메시지 상단 이미지
        private Integer imageWidth;     // 이미지 너비
        private Integer imageHeight;    // 이미지 높이
        private String link;            // 클릭 시 이동할 링크
        private Social social;          // 소셜 지표 영역
    }
    
    /**
     * 소셜 지표 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class Social {
        private Integer likeCount;      // 좋아요 수
        private Integer commentCount;   // 댓글 수
        private Integer shareCount;     // 공유 수
        private Integer viewCount;      // 조회수
    }
    
    /**
     * 버튼 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class Button {
        private String title;           // 버튼 제목
        private String link;            // 버튼 클릭 시 이동할 링크
        private String action;          // 버튼 액션 타입
    }
    
    /**
     * 피드 B형의 아이템 콘텐츠
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class ItemContent {
        private String profileText;     // 헤더 또는 프로필 텍스트
        private String profileImageUrl; // 프로필 이미지 URL
        private String titleImageText;  // 이미지 아이템 제목
        private String titleImageUrl;   // 이미지 아이템 이미지
        private String titleImageCategory; // 이미지 아이템 카테고리
        private List<Item> items;       // 텍스트 아이템 리스트 (최대 5개)
        private String sum;             // 요약 정보 제목
        private String sumOp;           // 요약 정보 설명
    }
    
    /**
     * 텍스트 아이템
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    public static class Item {
        private String title;           // 아이템 제목
        private String description;     // 아이템 설명
        private String link;            // 아이템 클릭 시 이동할 링크
    }
    
    /**
     * 뉴스 데이터로부터 피드 템플릿 생성
     */
    public static FeedTemplate createFromNews(List<NewsletterContent.Article> articles, FeedType feedType) {
        if (articles == null || articles.isEmpty()) {
            return createEmptyFeed();
        }
        
        FeedTemplateBuilder builder = FeedTemplate.builder()
                .feedType(feedType);
        
        // 메인 콘텐츠 구성
        NewsletterContent.Article mainArticle = articles.get(0);
        Content content = Content.builder()
                .title(mainArticle.getTitle())
                .description(mainArticle.getSummary())
                .imageUrl(mainArticle.getImageUrl())
                .imageWidth(800)
                .imageHeight(400)
                .link(mainArticle.getUrl())
                .social(createSocialFromArticle(mainArticle))
                .build();
        
        builder.content(content);
        
        // 버튼 구성
        List<Button> buttons = List.of(
                Button.builder()
                        .title("뉴스 보기")
                        .link(mainArticle.getUrl())
                        .action("web")
                        .build(),
                Button.builder()
                        .title("더 많은 뉴스")
                        .link("/news")
                        .action("web")
                        .build()
        );
        builder.buttons(buttons);
        
        // 피드 B형인 경우 아이템 콘텐츠 추가
        if (feedType == FeedType.FEED_B && articles.size() > 1) {
            List<ItemContent> itemContents = createItemContents(articles.subList(1, Math.min(articles.size(), 6)));
            builder.itemContents(itemContents);
        }
        
        return builder.build();
    }
    
    /**
     * 빈 피드 템플릿 생성
     */
    private static FeedTemplate createEmptyFeed() {
        return FeedTemplate.builder()
                .feedType(FeedType.FEED_A)
                .content(Content.builder()
                        .title("뉴스가 없습니다")
                        .description("현재 표시할 뉴스가 없습니다.")
                        .build())
                .build();
    }
    
    /**
     * 뉴스 아티클로부터 소셜 정보 생성
     */
    private static Social createSocialFromArticle(NewsletterContent.Article article) {
        return Social.builder()
                .viewCount(article.getViewCount() != null ? article.getViewCount().intValue() : 0)
                .shareCount(article.getShareCount() != null ? article.getShareCount().intValue() : 0)
                .likeCount(0) // 현재 시스템에 좋아요 기능이 없으므로 0
                .commentCount(0) // 현재 시스템에 댓글 기능이 없으므로 0
                .build();
    }
    
    /**
     * 아이템 콘텐츠 생성 (피드 B형용)
     */
    private static List<ItemContent> createItemContents(List<NewsletterContent.Article> articles) {
        if (articles.isEmpty()) {
            return List.of();
        }
        
        // 첫 번째 아이템을 헤더/프로필로 사용
        NewsletterContent.Article firstArticle = articles.get(0);
        ItemContent itemContent = ItemContent.builder()
                .profileText("관련 뉴스")
                .profileImageUrl(firstArticle.getImageUrl())
                .titleImageText(firstArticle.getTitle())
                .titleImageUrl(firstArticle.getImageUrl())
                .titleImageCategory(firstArticle.getCategory())
                .items(createItemsFromArticles(articles.subList(1, Math.min(articles.size(), 6))))
                .sum("총 " + articles.size() + "개의 뉴스")
                .sumOp("다양한 카테고리의 뉴스를 확인해보세요")
                .build();
        
        return List.of(itemContent);
    }
    
    /**
     * 뉴스 아티클로부터 아이템 리스트 생성
     */
    private static List<Item> createItemsFromArticles(List<NewsletterContent.Article> articles) {
        return articles.stream()
                .map(article -> Item.builder()
                        .title(article.getTitle())
                        .description(article.getSummary())
                        .link(article.getUrl())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 카카오톡 API용 템플릿 변수 생성
     */
    public Map<String, Object> toKakaoTemplateArgs() {
        Map<String, Object> templateObject = new java.util.HashMap<>();
        
        // object_type: "feed"로 고정
        templateObject.put("object_type", "feed");
        
        // content: 메인 콘텐츠 정보
        if (content != null) {
            Map<String, Object> contentMap = new java.util.HashMap<>();
            contentMap.put("title", content.getTitle());
            contentMap.put("description", content.getDescription());
            contentMap.put("image_url", content.getImageUrl());
            contentMap.put("image_width", content.getImageWidth());
            contentMap.put("image_height", content.getImageHeight());
            
            // link 객체 구성
            if (content.getLink() != null) {
                Map<String, Object> linkMap = new java.util.HashMap<>();
                linkMap.put("web_url", content.getLink());
                linkMap.put("mobile_web_url", content.getLink());
                contentMap.put("link", linkMap);
            }
            
            templateObject.put("content", contentMap);
        }
        
        // item_content: 피드 B형에서만 사용
        if (feedType == FeedType.FEED_B && itemContents != null && !itemContents.isEmpty()) {
            ItemContent itemContent = itemContents.get(0);
            Map<String, Object> itemContentMap = new java.util.HashMap<>();
            
            itemContentMap.put("profile_text", itemContent.getProfileText());
            itemContentMap.put("profile_image_url", itemContent.getProfileImageUrl());
            itemContentMap.put("title_image_url", itemContent.getTitleImageUrl());
            itemContentMap.put("title_image_text", itemContent.getTitleImageText());
            itemContentMap.put("title_image_category", itemContent.getTitleImageCategory());
            itemContentMap.put("sum", itemContent.getSum());
            itemContentMap.put("sum_op", itemContent.getSumOp());
            
            // items 배열 구성
            if (itemContent.getItems() != null && !itemContent.getItems().isEmpty()) {
                List<Map<String, Object>> itemsList = itemContent.getItems().stream()
                        .map(item -> {
                            Map<String, Object> itemMap = new java.util.HashMap<>();
                            itemMap.put("item", item.getTitle());
                            itemMap.put("item_op", item.getDescription());
                            return itemMap;
                        })
                        .collect(java.util.stream.Collectors.toList());
                itemContentMap.put("items", itemsList);
            }
            
            templateObject.put("item_content", itemContentMap);
        }
        
        // social: 소셜 정보
        if (content != null && content.getSocial() != null) {
            Map<String, Object> socialMap = new java.util.HashMap<>();
            socialMap.put("like_count", content.getSocial().getLikeCount());
            socialMap.put("comment_count", content.getSocial().getCommentCount());
            socialMap.put("shared_count", content.getSocial().getShareCount());
            socialMap.put("view_count", content.getSocial().getViewCount());
            templateObject.put("social", socialMap);
        }
        
        // buttons: 버튼 목록 (최대 2개)
        if (buttons != null && !buttons.isEmpty()) {
            List<Map<String, Object>> buttonList = buttons.stream()
                    .limit(2) // 최대 2개로 제한
                    .map(button -> {
                        Map<String, Object> buttonMap = new java.util.HashMap<>();
                        buttonMap.put("title", button.getTitle());
                        
                        // link 객체 구성
                        Map<String, Object> linkMap = new java.util.HashMap<>();
                        if ("web".equals(button.getAction())) {
                            linkMap.put("web_url", button.getLink());
                            linkMap.put("mobile_web_url", button.getLink());
                        } else {
                            // 앱 실행 파라미터
                            linkMap.put("android_execution_params", button.getLink());
                            linkMap.put("ios_execution_params", button.getLink());
                        }
                        buttonMap.put("link", linkMap);
                        
                        return buttonMap;
                    })
                    .collect(java.util.stream.Collectors.toList());
            templateObject.put("buttons", buttonList);
        }
        
        return templateObject;
    }
}
