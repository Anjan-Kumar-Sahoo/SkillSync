package com.skillsync.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:aksahoo1097@gmail.com}")
    private String fromEmail;

    @Async
    public void sendOtpEmail(String toEmail, String otp, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("SkillSync - Email Verification OTP");
            helper.setText(buildOtpHtml(otp, firstName), true);

            // Embed SkillSync logo as inline image
            ClassPathResource logo = new ClassPathResource("static/SkillSync_LOGO.png");
            if (logo.exists()) {
                helper.addInline("skillsync-logo", logo, "image/png");
            }

            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }

    private String buildOtpHtml(String otp, String firstName) {
        return """
            <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 500px; margin: 0 auto;
                        padding: 30px; background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%);
                        border-radius: 16px;">
                <div style="background: white; border-radius: 12px; padding: 30px; text-align: center;">
                    <div style="margin-bottom: 15px;">
                        <img src="cid:skillsync-logo" alt="SkillSync" width="120" height="120"
                             style="display: block; margin: 0 auto; border-radius: 8px;">
                    </div>
                    <h1 style="color: #333; font-size: 24px; margin-bottom: 5px; margin-top: 10px;">
                        SkillSync
                    </h1>
                    <h2 style="color: #555; font-size: 18px; margin-bottom: 20px;">
                        Email Verification
                    </h2>
                    <p style="color: #666; font-size: 14px;">
                        Hi <strong>%s</strong>, use the OTP below to verify your email address:
                    </p>
                    <div style="background: #f0f4ff; border: 2px dashed #667eea; border-radius: 10px;
                                padding: 20px; margin: 20px 0;">
                        <span style="font-size: 36px; font-weight: bold; letter-spacing: 8px; color: #667eea;">
                            %s
                        </span>
                    </div>
                    <p style="color: #999; font-size: 12px;">
                        This OTP is valid for <strong>5 minutes</strong>. Do not share it with anyone.
                    </p>
                    <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
                    <p style="color: #bbb; font-size: 11px;">
                        If you didn't request this, please ignore this email.
                    </p>
                </div>
            </div>
            """.formatted(firstName, otp);
    }
}

