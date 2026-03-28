package com.homecare.notification.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Sends HTML emails using JavaMail + Thymeleaf templates.
 * All sends are async to avoid blocking the calling thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${homecare.notification.email.from:noreply@homecare.in}")
    private String fromAddress;

    @Value("${homecare.notification.email.enabled:false}")
    private boolean emailEnabled;

    /**
     * Sends an HTML email rendered from a Thymeleaf template.
     *
     * @param to           recipient email
     * @param subject      email subject
     * @param templateName template file name (without .html) under templates/email/
     * @param variables    model variables for the template
     */
    @Async
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        if (!emailEnabled) {
            log.info("Email disabled — would send '{}' to {} using template '{}'", subject, to, templateName);
            logEmailVariables(to, subject, templateName, variables);
            return;
        }

        try {
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process("email/" + templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent: '{}' to {}", subject, to);
        } catch (MessagingException e) {
            log.error("Failed to send email '{}' to {}: {}", subject, to, e.getMessage());
        }
    }

    private void logEmailVariables(String to, String subject, String templateName, Map<String, Object> variables) {
        log.debug("═══ EMAIL (dev-mode) ═══");
        log.debug("  To:       {}", to);
        log.debug("  Subject:  {}", subject);
        log.debug("  Template: {}", templateName);
        log.debug("  Vars:     {}", variables);
        log.debug("════════════════════════");
    }
}

