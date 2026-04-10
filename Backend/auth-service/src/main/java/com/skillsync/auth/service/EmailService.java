package com.skillsync.auth.service;

import com.skillsync.auth.enums.OtpType;
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

    @Value("${app.base-url:https://skillsync.mraks.dev}")
    private String appBaseUrl;

    @Async
    public void sendOtpEmail(String toEmail, String otp, String firstName, OtpType otpType) {
        String safeName = (firstName == null || firstName.isBlank()) ? "there" : firstName;
        boolean passwordReset = otpType == OtpType.PASSWORD_RESET;

        String subject = passwordReset
                ? "SkillSync - Password Reset OTP"
                : "SkillSync - Email Verification OTP";

        String title = passwordReset ? "Reset Your Password" : "Verify Your Email";
        String primaryMessage = passwordReset
                ? "Use the one-time password below to securely reset your SkillSync password."
                : "Use the one-time password below to complete your SkillSync email verification.";

        String otpCard = """
                <div style="margin: 12px 0 6px 0;">
                  <div style="font-size: 12px; color: #475569; text-transform: uppercase; letter-spacing: 0.08em; font-weight: 700;">One-Time Password</div>
                  <div style="margin-top: 8px; display: inline-block; background: #eef2ff; border: 2px dashed #4f46e5; border-radius: 12px; padding: 12px 18px; font-size: 30px; font-weight: 800; letter-spacing: 0.28em; color: #312e81;">%s</div>
                </div>
                """.formatted(escapeHtml(otp));

        String details = """
                %s
                <p style="margin: 12px 0 0 0; color: #475569; font-size: 14px; line-height: 1.6;">
                  This OTP is valid for <strong>5 minutes</strong>. For security, never share this code with anyone.
                </p>
                """.formatted(otpCard);

        String ctaUrl = passwordReset ? appBaseUrl + "/forgot-password" : appBaseUrl + "/verify-otp";
        String ctaText = passwordReset ? "Open Password Reset" : "Open Verification";

        String html = buildEmailTemplate(
                safeName,
                title,
                primaryMessage,
                "Important details",
                details,
                ctaText,
                ctaUrl,
                "Need help? Reply to this email and our support team will assist you."
        );

        sendHtmlEmail(toEmail, subject, html, passwordReset ? "PASSWORD_RESET_OTP" : "OTP_VERIFICATION");
    }

    @Async
    public void sendWelcomeEmail(String toEmail, String firstName) {
        String safeName = (firstName == null || firstName.isBlank()) ? "there" : firstName;
        String html = buildEmailTemplate(
                safeName,
                "Welcome to SkillSync",
                "Your account is now ready. Discover mentors, book sessions, and start learning with confidence.",
                "What you can do next",
                """
                <ul style="margin: 0; padding-left: 18px; color: #334155; font-size: 14px; line-height: 1.7;">
                  <li>Explore mentors by skill and rating.</li>
                  <li>Book a session in minutes with secure checkout.</li>
                  <li>Track sessions, earnings, and notifications in one dashboard.</li>
                </ul>
                """,
                "Go to SkillSync",
                appBaseUrl + "/dashboard",
                "Welcome aboard. We are excited to be part of your growth journey."
        );

        sendHtmlEmail(toEmail, "Welcome to SkillSync", html, "WELCOME_EMAIL");
    }

    private void sendHtmlEmail(String toEmail, String subject, String htmlContent, String emailType) {
        log.info("[EMAIL] Sending email to: {} | emailType={}", toEmail, emailType);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            ClassPathResource logo = new ClassPathResource("static/SkillSync_LOGO.png");
            if (logo.exists()) {
                helper.addInline("skillsync-logo", logo, "image/png");
            }

            mailSender.send(message);
            log.info("[EMAIL] Email sent successfully | to={} | emailType={}", toEmail, emailType);
        } catch (MessagingException e) {
            log.error("[EMAIL] Failed to send email | to={} | emailType={} | error={}", toEmail, emailType, e.getMessage());
        } catch (Exception e) {
            log.error("[EMAIL] Unexpected error sending email | to={} | emailType={} | error={}", toEmail, emailType, e.getMessage());
        }
    }

    private String buildEmailTemplate(
            String recipientName,
            String title,
            String primaryMessage,
            String detailsTitle,
            String detailsHtml,
            String actionText,
            String actionUrl,
            String footerNote
    ) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <meta name="color-scheme" content="light dark" />
                  <meta name="supported-color-schemes" content="light dark" />
                  <title>SkillSync Email</title>
                </head>
                <body style="margin:0;padding:0;background-color:#f1f5f9;font-family:Segoe UI,Arial,sans-serif;color:#0f172a;">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background-color:#f1f5f9;padding:24px 12px;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:620px;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e2e8f0;">
                          <tr>
                            <td style="padding:24px;background:linear-gradient(135deg,#4f46e5,#7c3aed);text-align:center;">
                              <img src="cid:skillsync-logo" alt="SkillSync logo" width="64" height="64" style="display:block;margin:0 auto 10px auto;border-radius:12px;" />
                              <div style="font-size:24px;font-weight:800;color:#ffffff;letter-spacing:0.02em;">SkillSync</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:26px 24px 22px 24px;">
                              <p style="margin:0 0 10px 0;font-size:14px;color:#475569;">Hi %s,</p>
                              <h1 style="margin:0 0 12px 0;font-size:28px;line-height:1.25;color:#0f172a;">%s</h1>
                              <p style="margin:0;font-size:15px;line-height:1.7;color:#334155;">%s</p>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:0 24px;">
                              <div style="border:1px solid #cbd5e1;border-radius:12px;background:#f8fafc;padding:16px 16px 14px 16px;">
                                <div style="font-size:12px;font-weight:800;text-transform:uppercase;letter-spacing:0.08em;color:#475569;margin-bottom:8px;">%s</div>
                                %s
                              </div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:22px 24px 10px 24px;text-align:center;">
                              <a href="%s" style="display:inline-block;background:#4f46e5;color:#ffffff;text-decoration:none;padding:12px 22px;border-radius:10px;font-size:14px;font-weight:700;">%s</a>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:12px 24px;">
                              <hr style="border:none;border-top:1px solid #e2e8f0;margin:0;" />
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:0 24px 24px 24px;">
                              <p style="margin:0 0 8px 0;font-size:13px;line-height:1.6;color:#64748b;">%s</p>
                              <p style="margin:0;font-size:12px;color:#94a3b8;">SkillSync Support • support@skillsync.mraks.dev</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                escapeHtml(recipientName),
                escapeHtml(title),
                escapeHtml(primaryMessage),
                escapeHtml(detailsTitle),
                detailsHtml,
                escapeHtml(actionUrl),
                escapeHtml(actionText),
                escapeHtml(footerNote)
        );
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
