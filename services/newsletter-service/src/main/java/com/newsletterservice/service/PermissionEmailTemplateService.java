package com.newsletterservice.service;

import com.newsletterservice.client.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 권한 설정 안내 이메일 템플릿 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionEmailTemplateService {
    
    /**
     * 카카오톡 권한 설정 안내 이메일 HTML 생성
     * 
     * @param user 사용자 정보
     * @return 이메일 HTML 내용
     */
    public String generatePermissionEmailHtml(UserResponse user) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>");
        html.append("<html lang='ko'>");
        html.append("<head>");
        html.append("    <meta charset='UTF-8'>");
        html.append("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        html.append("    <title>카카오톡 알림 권한 설정 안내</title>");
        html.append("    <style>");
        html.append("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }");
        html.append("        .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }");
        html.append("        .header { background: linear-gradient(135deg, #FEE500 0%, #FF6B35 100%); color: #3C1E1E; padding: 30px; text-align: center; }");
        html.append("        .header h1 { margin: 0; font-size: 24px; font-weight: 600; }");
        html.append("        .content { padding: 30px; }");
        html.append("        .section { margin-bottom: 25px; }");
        html.append("        .section h2 { color: #3C1E1E; font-size: 18px; margin-bottom: 15px; border-bottom: 2px solid #FEE500; padding-bottom: 8px; }");
        html.append("        .step { background-color: #f8f9fa; padding: 15px; border-radius: 6px; margin-bottom: 15px; border-left: 4px solid #FEE500; }");
        html.append("        .step-number { background-color: #FEE500; color: #3C1E1E; width: 24px; height: 24px; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; font-weight: bold; margin-right: 10px; }");
        html.append("        .cta-button { display: inline-block; background: linear-gradient(135deg, #FEE500 0%, #FF6B35 100%); color: #3C1E1E; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: 600; margin: 20px 0; }");
        html.append("        .cta-button:hover { opacity: 0.9; }");
        html.append("        .footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #6c757d; font-size: 14px; }");
        html.append("        .highlight { background-color: #fff3cd; padding: 10px; border-radius: 4px; border-left: 4px solid #ffc107; margin: 15px 0; }");
        html.append("    </style>");
        html.append("</head>");
        html.append("<body>");
        html.append("    <div class='container'>");
        html.append("        <div class='header'>");
        html.append("            <h1>📱 카카오톡 알림 권한 설정 안내</h1>");
        html.append("        </div>");
        html.append("        <div class='content'>");
        html.append("            <div class='section'>");
        html.append("                <p>안녕하세요, ").append(user.getNickname() != null ? user.getNickname() : "사용자").append("님!</p>");
        html.append("                <p>뉴스레터를 카카오톡으로 받으시려면 <strong>카카오톡 메시지 전송 권한</strong>이 필요합니다.</p>");
        html.append("            </div>");
        html.append("            <div class='section'>");
        html.append("                <h2>🔧 권한 설정 방법</h2>");
        html.append("                <div class='step'>");
        html.append("                    <span class='step-number'>1</span>");
        html.append("                    <strong>설정 페이지로 이동</strong><br>");
        html.append("                    아래 버튼을 클릭하여 권한 설정 페이지로 이동하세요.");
        html.append("                </div>");
        html.append("                <div class='step'>");
        html.append("                    <span class='step-number'>2</span>");
        html.append("                    <strong>카카오 계정 재연결</strong><br>");
        html.append("                    카카오 계정을 다시 연결하고 '카카오톡 메시지 전송' 권한에 동의해주세요.");
        html.append("                </div>");
        html.append("                <div class='step'>");
        html.append("                    <span class='step-number'>3</span>");
        html.append("                    <strong>완료</strong><br>");
        html.append("                    권한 설정이 완료되면 다음 뉴스레터부터 카카오톡으로 받으실 수 있습니다.");
        html.append("                </div>");
        html.append("            </div>");
        html.append("            <div class='section' style='text-align: center;'>");
        html.append("                <a href='/settings/permissions' class='cta-button'>권한 설정하러 가기</a>");
        html.append("            </div>");
        html.append("            <div class='highlight'>");
        html.append("                <strong>💡 참고사항</strong><br>");
        html.append("                • 권한 설정을 하지 않으시면 이메일로 뉴스레터를 받으실 수 있습니다.<br>");
        html.append("                • 언제든지 설정에서 알림 방식을 변경하실 수 있습니다.");
        html.append("            </div>");
        html.append("        </div>");
        html.append("        <div class='footer'>");
        html.append("            <p>이 메일은 뉴스레터 서비스에서 발송되었습니다.</p>");
        html.append("            <p>문의사항이 있으시면 고객센터로 연락해주세요.</p>");
        html.append("        </div>");
        html.append("    </div>");
        html.append("</body>");
        html.append("</html>");
        
        return html.toString();
    }
}
