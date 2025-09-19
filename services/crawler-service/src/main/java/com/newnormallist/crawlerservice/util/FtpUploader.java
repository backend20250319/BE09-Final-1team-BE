package com.newnormallist.crawlerservice.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * FTP 파일 업로드 유틸리티 (Apache Commons Net 사용)
 * 
 * 역할:
 * - FTP 서버에 CSV 파일 업로드
 * - 디렉터리 자동 생성
 * - 안정적인 FTP 연결 관리
 * 
 * 기능:
 * - CSV 문자열을 FTP 서버에 업로드
 * - MultipartFile을 FTP 서버에 업로드
 * - 디렉터리 자동 생성
 * - Passive 모드 및 Binary 모드 지원
 */
@Slf4j
public class FtpUploader {

    /**
     * CSV 문자열을 FTP 서버에 업로드
     * 
     * @param server FTP 서버 주소
     * @param port FTP 포트
     * @param user 사용자명
     * @param password 비밀번호
     * @param remoteDir 원격 디렉터리 경로
     * @param filename 파일명
     * @param csvContent CSV 내용
     * @return 업로드 성공 여부
     */
    public static boolean uploadCsvFile(String server, int port, String user, String password,
                                       String remoteDir, String filename, String csvContent) {
        
        FTPClient ftpClient = new FTPClient();
        
        try (InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes("UTF-8"))) {
            // 1. FTP 서버 연결
            ftpClient.connect(server, port);
            log.debug("FTP 서버 연결 성공: {}:{}", server, port);
            
            // 2. 로그인
            boolean loginSuccess = ftpClient.login(user, password);
            if (!loginSuccess) {
                log.error("FTP 로그인 실패: {}", user);
                return false;
            }
            log.debug("FTP 로그인 성공: {}", user);
            
            // 3. Passive 모드 설정
            ftpClient.enterLocalPassiveMode();
            
            // 4. Binary 파일 타입 설정
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            
            // 5. 디렉터리 생성 및 이동
            boolean dirSuccess = createAndChangeDirectory(ftpClient, remoteDir);
            if (!dirSuccess) {
                log.error("디렉터리 생성/이동 실패: {}", remoteDir);
                return false;
            }
            
            // 6. 파일 업로드
            boolean uploadSuccess = ftpClient.storeFile(filename, inputStream);
            
            if (uploadSuccess) {
                log.info("📁 FTP 업로드 성공: {}/{}", remoteDir, filename);
            } else {
                log.error("📁 FTP 업로드 실패: {}/{}", remoteDir, filename);
            }
            
            // 7. 로그아웃
            ftpClient.logout();
            return uploadSuccess;
            
        } catch (IOException e) {
            log.error("📁 FTP 업로드 오류: {}/{}, 오류: {}", remoteDir, filename, e.getMessage());
            return false;
        } finally {
            // 8. 연결 종료
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                log.warn("FTP 연결 종료 오류: {}", e.getMessage());
            }
        }
    }
    
    /**
     * MultipartFile을 FTP 서버에 업로드
     * 
     * @param server FTP 서버 주소
     * @param port FTP 포트
     * @param user 사용자명
     * @param password 비밀번호
     * @param remoteDir 원격 디렉터리 경로
     * @param file 업로드할 파일
     * @return 업로드 성공 여부
     */
    public static boolean uploadFile(String server, int port, String user, String password,
                                   String remoteDir, MultipartFile file) {
        
        FTPClient ftpClient = new FTPClient();
        
        try (InputStream inputStream = file.getInputStream()) {
            // 1. FTP 서버 연결
            ftpClient.connect(server, port);
            log.debug("FTP 서버 연결 성공: {}:{}", server, port);
            
            // 2. 로그인
            boolean loginSuccess = ftpClient.login(user, password);
            if (!loginSuccess) {
                log.error("FTP 로그인 실패: {}", user);
                return false;
            }
            log.debug("FTP 로그인 성공: {}", user);
            
            // 3. Passive 모드 설정
            ftpClient.enterLocalPassiveMode();
            
            // 4. Binary 파일 타입 설정
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            
            // 5. 디렉터리 생성 및 이동
            boolean dirSuccess = createAndChangeDirectory(ftpClient, remoteDir);
            if (!dirSuccess) {
                log.error("디렉터리 생성/이동 실패: {}", remoteDir);
                return false;
            }
            
            // 6. 파일 업로드
            boolean uploadSuccess = ftpClient.storeFile(file.getOriginalFilename(), inputStream);
            
            if (uploadSuccess) {
                log.info("📁 FTP 파일 업로드 성공: {}/{}", remoteDir, file.getOriginalFilename());
            } else {
                log.error("📁 FTP 파일 업로드 실패: {}/{}", remoteDir, file.getOriginalFilename());
            }
            
            // 7. 로그아웃
            ftpClient.logout();
            return uploadSuccess;
            
        } catch (IOException e) {
            log.error("📁 FTP 파일 업로드 오류: {}/{}, 오류: {}", remoteDir, file.getOriginalFilename(), e.getMessage());
            return false;
        } finally {
            // 8. 연결 종료
            try {
                if (ftpClient.isConnected()) {
                    ftpClient.disconnect();
                }
            } catch (IOException e) {
                log.warn("FTP 연결 종료 오류: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 디렉터리 재귀적 생성 및 이동
     * 
     * @param ftpClient FTP 클라이언트
     * @param remotePath 원격 경로
     * @return 성공 여부
     */
    private static boolean createAndChangeDirectory(FTPClient ftpClient, String remotePath) {
        try {
            // 경로를 "/" 기준으로 분할
            String[] directories = remotePath.split("/");
            
            for (String dir : directories) {
                if (dir.isEmpty()) continue;
                
                // 디렉터리 이동 시도
                boolean changed = ftpClient.changeWorkingDirectory(dir);
                
                if (!changed) {
                    // 디렉터리가 없으면 생성
                    boolean created = ftpClient.makeDirectory(dir);
                    if (created) {
                        log.debug("디렉터리 생성 성공: {}", dir);
                        // 생성 후 이동
                        changed = ftpClient.changeWorkingDirectory(dir);
                    } else {
                        log.warn("디렉터리 생성 실패: {}", dir);
                        return false;
                    }
                }
                
                if (!changed) {
                    log.error("디렉터리 이동 실패: {}", dir);
                    return false;
                }
                
                log.debug("디렉터리 이동 성공: {}", dir);
            }
            
            return true;
            
        } catch (IOException e) {
            log.error("디렉터리 생성/이동 오류: {}", e.getMessage());
            return false;
        }
    }
}