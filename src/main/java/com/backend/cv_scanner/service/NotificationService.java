package com.backend.cv_scanner.service;

import com.backend.cv_scanner.dao.entity.CVBatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

@Slf4j
@Service
public class NotificationService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean enabled;
    private final String from;
    private final String hrEmail;

    public NotificationService(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.notification.enabled:false}") boolean enabled,
            @Value("${app.notification.from:noreply@cvscanner.local}") String from,
            @Value("${app.notification.hr-email:}") String hrEmail
    ) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.hrEmail = hrEmail;
    }

    /**
     * Notifies the uploader (and the configured HR mailbox) that a batch finished.
     * A mail failure is only logged: it must never fail the batch job itself.
     */
    public void sendBatchCompleted(CVBatch batch) {

        if (!enabled) {
            return;
        }

        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            log.warn("Notifications are enabled but no mail server is configured (spring.mail.host)");
            return;
        }

        Set<String> recipients = new LinkedHashSet<>();
        if (batch.getCreatedBy() != null) {
            recipients.add(batch.getCreatedBy());
        }
        if (!hrEmail.isBlank()) {
            recipients.add(hrEmail);
        }
        if (recipients.isEmpty()) {
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(recipients.toArray(String[]::new));
        message.setSubject("CV batch '" + batch.getOriginalFilename() + "' finished: " + batch.getStatus());
        message.setText(buildBody(batch));

        try {
            sender.send(message);
            log.info("Sent completion email for batch {} to {}", batch.getId(), recipients);
        } catch (MailException e) {
            log.error("Failed to send completion email for batch {}", batch.getId(), e);
        }
    }

    private String buildBody(CVBatch batch) {
        String duration = batch.getStartedAt() != null && batch.getFinishedAt() != null
                ? Duration.between(batch.getStartedAt(), batch.getFinishedAt()).toSeconds() + "s"
                : "-";

        return """
                Hello,

                Processing of CV batch "%s" has finished.

                Status:          %s
                Total CVs:       %d
                Parsed:          %d
                Failed/skipped:  %d
                Duration:        %s

                Batch id: %s
                Details: GET /api/cv-batches/%s
                Failed files: GET /api/cv-batches/%s/files?status=FAILED

                -- CVScanner
                """.formatted(
                batch.getOriginalFilename(),
                batch.getStatus(),
                batch.getTotal(),
                batch.getProcessedFiles(),
                batch.getFailedFiles(),
                duration,
                batch.getId(),
                batch.getId(),
                batch.getId()
        );
    }
}
