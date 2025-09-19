package com.newnormallist.crawlerservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * FTP 서버 설정을 위한 Configuration 클래스
 * 
 * 역할:
 * - application-secret.yml에서 FTP 설정 정보를 로드
 * - 하드코딩된 FTP 정보를 제거하여 보안 강화
 * - 환경별 설정 변경 용이성 제공
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ftp")
public class FtpConfig {
    
    /**
     * FTP 서버 주소
     */
    private String server;
    
    /**
     * FTP 포트
     */
    private int port;
    
    /**
     * FTP 사용자명
     */
    private String username;
    
    /**
     * FTP 비밀번호
     */
    private String password;
    
    /**
     * FTP 기본 경로
     */
    private String basePath;
}

