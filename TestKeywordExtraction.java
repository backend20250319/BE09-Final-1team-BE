import java.util.*;
import java.util.regex.Pattern;

public class TestKeywordExtraction {
    
    public static void main(String[] args) {
        TestKeywordExtraction test = new TestKeywordExtraction();
        
        // 테스트 텍스트들
        String[] testTexts = {
            "AI 기술이 발전하면서 자동차 산업에 큰 변화가 일어나고 있다",
            "트럼프 대통령이 새로운 정책을 발표했다고 밝혔다",
            "현대차가 전기차 판매량을 늘리기 위해 노력하고 있다",
            "서울에서 열린 IT 컨퍼런스에서 AI 관련 발표가 있었다",
            "이번 주 경제 뉴스에서는 주식 시장 동향을 분석했다"
        };
        
        for (int i = 0; i < testTexts.length; i++) {
            System.out.println("=== 테스트 " + (i+1) + " ===");
            System.out.println("원본 텍스트: " + testTexts[i]);
            List<String> keywords = test.extractKeywordsFromText(testTexts[i]);
            System.out.println("추출된 키워드: " + keywords);
            System.out.println();
        }
    }
    
    public List<String> extractKeywordsFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        List<String> keywords = new ArrayList<>();
        
        // 복합 명사 처리 (IT 컨퍼런스, 주식 시장 등)
        text = text.replaceAll("IT 컨퍼런스", "IT컨퍼런스");
        text = text.replaceAll("주식 시장", "주식시장");
        
        String[] words = text.split("\\s+");
        
        for (String word : words) {
            if (word == null || word.trim().isEmpty()) {
                continue;
            }
            
            String cleanedWord = word.replaceAll("[^가-힣0-9A-Za-z]", "");
            
            if (isValidKeyword(cleanedWord)) {
                // 조사나 어미 제거
                String normalizedWord = removeJosaAndEndings(cleanedWord);
                if (!normalizedWord.isEmpty() && !isStopWord(normalizedWord)) {
                    keywords.add(normalizedWord);
                }
            }
        }
        
        return keywords;
    }
    
    /**
     * 조사나 어미 제거
     */
    private String removeJosaAndEndings(String word) {
        if (word.length() < 2) {
            return word;
        }
        
        // 조사 제거
        String[] josa = {"이", "가", "을", "를", "의", "에", "에서", "로", "으로", "와", "과", "도", "는", "은", "만", "부터", "까지", "에게", "로서", "같이", "처럼", "만큼", "보다", "같은"};
        for (String j : josa) {
            if (word.endsWith(j)) {
                word = word.substring(0, word.length() - j.length());
                break;
            }
        }
        
        // 동사 어미 제거
        String[] verbEndings = {"하면서", "하고", "하며", "이다", "있다", "없다", "하다", "되다", "따르다", "통하다", "위하다", "대하다", "했다고", "했다", "했다", "하고", "하며", "면서", "이다", "있다", "없다", "하다", "되다", "따르다", "통하다", "위하다", "대하다"};
        for (String ending : verbEndings) {
            if (word.endsWith(ending)) {
                word = word.substring(0, word.length() - ending.length());
                break;
            }
        }
        
        // 일반적인 어미 제거
        String[] endings = {"는", "은", "이", "가", "을", "를", "의", "에", "에서", "로", "으로", "와", "과", "도", "만", "부터", "까지", "에게", "로서", "같이", "처럼", "만큼", "보다", "같은"};
        for (String ending : endings) {
            if (word.endsWith(ending)) {
                word = word.substring(0, word.length() - ending.length());
                break;
            }
        }
        
        return word;
    }
    
    private boolean isValidKeyword(String word) {
        if (word.length() < 2) {
            return false;
        }
        
        if (isStopWord(word)) {
            return false;
        }
        
        if (word.matches("^\\d+$")) {
            if (word.length() == 4 && word.startsWith("20")) {
                return true;
            }
            return false;
        }
        
        // 영문+한글 조합 허용 (IT컨퍼런스 등)
        if (word.matches("^[A-Za-z]+[가-힣]+$") || word.matches("^[가-힣]+[A-Za-z]+$")) {
            return true;
        }
        
        if (word.matches("^[A-Za-z]+$")) {
            if (word.length() <= 2) {
                return false;
            }
            Set<String> englishStopWords = Set.of(
                "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by",
                "is", "are", "was", "were", "be", "been", "being", "have", "has", "had", "do", "does", "did",
                "will", "would", "could", "should", "may", "might", "can", "must", "shall",
                "this", "that", "these", "those", "it", "its", "they", "them", "their", "we", "us", "our",
                "you", "your", "he", "him", "his", "she", "her", "hers", "i", "me", "my", "mine"
            );
            if (englishStopWords.contains(word.toLowerCase())) {
                return false;
            }
        }
        
        if (word.matches(".*[가-힣]$")) {
            if (word.endsWith("으로") || word.endsWith("에서") || word.endsWith("에게") || 
                word.endsWith("부터") || word.endsWith("까지") || word.endsWith("로서") ||
                word.endsWith("같이") || word.endsWith("처럼") || word.endsWith("만큼") ||
                word.endsWith("보다") || word.endsWith("같은") || word.endsWith("것") ||
                word.endsWith("수") || word.endsWith("등") || word.endsWith("및")) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean isStopWord(String word) {
        Set<String> stopWords = new HashSet<>(Arrays.asList(
            // 기본 스톱워드
            "있다", "없다", "하다", "되다", "이다", "것", "등", "및", "또는", "그리고",
            "이번", "지난", "현재", "오늘", "내일", "어제", "내년", "작년", "올해", "금년",
            "현장", "관련", "기자", "사진", "영상", "단독", "인터뷰", "종합",
            "정부", "대통령", "국회", "한국", "대한민국", "뉴스", "기사", "외신",
            "밝혔다", "통해", "위해", "대해", "에서", "으로", "에게", "부터", "까지", "로서",
            "같이", "처럼", "만큼", "보다", "같은", "이런", "저런", "그런", "어떤", "무슨",
            "몇", "얼마", "언제", "어디", "누가", "무엇", "어떻게", "왜", "어째서",
            "있는", "없는", "하는", "되는", "등을", "것을", "것이", "것은",
            "것도", "것만", "것과", "것의", "것에", "때문", "이유", "결과", "원인", "목적", "방법",
            
            // 사용자가 요청한 제외 키워드들
            "것으로", "서울", "대한", "따르면", "당시", "12일",
            
            // 날짜 관련
            "월", "일", "년", "분", "초", "주", "개월", "년도", "년간", "개월간", "주간",
            "오전", "오후", "새벽", "밤", "낮", "저녁", "아침", "점심", "식사",
            
            // 요일
            "월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일",
            "화", "수", "목", "금", "토",
            
            // 장소 관련
            "부산", "대구", "인천", "광주", "대전", "울산", "세종", "제주",
            "경기", "강원", "충북", "충남", "전북", "전남", "경북", "경남",
            "강남", "강북", "서초", "마포", "종로", "중구", "용산", "성동", "광진", "동대문", "중랑", "성북", "도봉", "노원", "은평", "서대문", "양천", "강서", "구로", "금천", "영등포", "동작", "관악", "송파", "강동",
            "지역", "지방", "군", "읍", "면", "리", "번지", "길", "가", "호",
            
            // 조사
            "이", "가", "을", "를", "의", "에", "에서", "로", "와", "과", "도", "는", "은", "만", "부터", "까지", "에게", "로서", "같이", "처럼", "만큼", "보다", "같은", "이런", "저런", "그런", "어떤", "무슨", "몇", "얼마", "언제", "어디", "누가", "무엇", "어떻게", "왜", "어째서",
            
            // 어미
            "따르다", "통하다", "위하다", "대하다",
            "등을", "등이", "등은", "등도", "등과", "등에", "등의",
            "따른", "따르는", "따를", "따른다", "통한", "통하는", "통할", "통한다", "위한", "위하는", "위할", "위한다", "대한", "대하는", "대할", "대한다"
        ));
        
        return stopWords.contains(word);
    }
}
