package com.newnormallist.crawlerservice.controller;

import com.newnormallist.crawlerservice.config.FtpConfig;
import com.newnormallist.crawlerservice.util.FtpUploader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



/**
 * FTP 파일 업로드 컨트롤러
 * 
 * 역할:
 * - CSV 파일을 FTP 서버에 업로드
 * - 폴더 구조: /1/am|pm/yyyy-MM-dd_am|pm/stage/
 * - 크롤러 서비스 내부에서 사용하는 FTP 업로드 API
 * 
 * 기능:
 * - POST /api/ftp/upload: CSV 파일 업로드
 * - 디렉터리 자동 생성
 * - 파일 덮어쓰기 지원
 */
@Tag(name = "FTP Upload", description = "FTP 파일 업로드 API")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ftp")
public class FtpUploadController {

    private final FtpConfig ftpConfig;

    @Operation(summary = "CSV 파일 업로드", description = "JSON 형태로 CSV 파일을 FTP 서버에 업로드합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업로드 성공"),
            @ApiResponse(responseCode = "500", description = "업로드 실패")
    })
    @PostMapping("/upload")
    public ResponseEntity<String> uploadCsv(
            @Parameter(description = "업로드 요청 (경로, 파일명, 내용)") @RequestBody CsvUploadRequest request) {
        try {
            // FTP 경로 구성: basePath + 상대경로
            String ftpPath = ftpConfig.getBasePath() + "/" + request.getPath();
            
            boolean result = FtpUploader.uploadCsvFile(
                ftpConfig.getServer(),    // FTP 서버 (설정파일에서)
                ftpConfig.getPort(),      // 포트 (설정파일에서)
                ftpConfig.getUsername(),  // 사용자 (설정파일에서)
                ftpConfig.getPassword(),  // 비밀번호 (설정파일에서)
                ftpPath,                  // FTP 경로
                request.getFilename(),    // 파일명
                request.getContent()      // CSV 내용
            );

            if (result) {
                log.info("📁 FTP 업로드 성공: {}/{}", ftpPath, request.getFilename());
                return ResponseEntity.ok("업로드 성공");
            } else {
                log.error("📁 FTP 업로드 실패: {}/{}", ftpPath, request.getFilename());
                return ResponseEntity.status(500).body("업로드 실패");
            }
            
        } catch (Exception e) {
            log.error("📁 FTP 업로드 오류: {}, 오류: {}", request.getFilename(), e.getMessage());
            return ResponseEntity.status(500).body("업로드 오류: " + e.getMessage());
        }
    }

    @Operation(summary = "파일 업로드 (테스트용)", description = "MultipartFile 형태로 파일을 FTP 서버에 업로드합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "파일 업로드 성공"),
            @ApiResponse(responseCode = "500", description = "파일 업로드 실패")
    })
    @PostMapping("/upload-file")
    public ResponseEntity<String> uploadFile(
            @Parameter(description = "업로드할 파일") @RequestParam("file") MultipartFile file,
            @Parameter(description = "FTP 상대 경로", example = "pm/2025-08-19_pm/list") @RequestParam("path") String path) {
        
        try {
            // FTP 경로 구성: basePath + 상대경로
            String ftpPath = ftpConfig.getBasePath() + "/" + path;
            
            boolean result = FtpUploader.uploadFile(
                ftpConfig.getServer(),    // FTP 서버 (설정파일에서)
                ftpConfig.getPort(),      // 포트 (설정파일에서)
                ftpConfig.getUsername(),  // 사용자 (설정파일에서)
                ftpConfig.getPassword(),  // 비밀번호 (설정파일에서)
                ftpPath,                  // FTP 경로
                file                      // 파일
            );

            if (result) {
                log.info("📁 FTP 파일 업로드 성공: {}/{}", ftpPath, file.getOriginalFilename());
                return ResponseEntity.ok("파일 업로드 성공");
            } else {
                log.error("📁 FTP 파일 업로드 실패: {}/{}", ftpPath, file.getOriginalFilename());
                return ResponseEntity.status(500).body("파일 업로드 실패");
            }
            
        } catch (Exception e) {
            log.error("📁 FTP 파일 업로드 오류: {}, 오류: {}", file.getOriginalFilename(), e.getMessage());
            return ResponseEntity.status(500).body("파일 업로드 오류: " + e.getMessage());
        }
    }

    /**
     * CSV 업로드 요청 DTO
     */
    public static class CsvUploadRequest {
        private String path;      // 상대 경로 (예: "pm/2025-08-19_pm/list")
        private String filename;  // 파일명 (예: "politics_list_2025-08-19-15-26.csv")
        private String content;   // 파일 내용 (CSV 데이터)
        
        // Getters and Setters
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }
}
