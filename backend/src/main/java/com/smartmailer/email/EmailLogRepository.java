package com.smartmailer.email;

import com.smartmailer.recipient.Recipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findByRecipientInOrderByCreatedAtDesc(List<Recipient> recipients);
}
