package at.refugeescode.checkin.service;

import com.google.common.base.Strings;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class MailService {

    public static InternetAddress parse(String address) throws AddressException {
        return address == null || address.isEmpty() ? null : new InternetAddress(address);
    }

    @NonNull
    private final JavaMailSender mailSender;

    @Value("${checkin.mail.enabled}")
    private boolean enabled;
    @Value("${checkin.mail.from}")
    private String from;
    @Value("${checkin.mail.debug}")
    private boolean debug;
    @Value("${checkin.mail.webmaster}")
    private String webmaster;

    private final Queue<MimeMessage> messageQueue = new ConcurrentLinkedQueue<>();

    @Async
    public void sendMail(String to, String replyTo, String bcc, String subject, String text) {
        sendMail(this.from, to, replyTo, bcc, subject, text);
    }

    @Async
    public void sendMail(String from, String to, String replyTo, String bcc, String subject, String text) {
        if (debug) {
            to = webmaster;
        }
        if (Strings.isNullOrEmpty(to)) {
            log.warn("to is null or empty");
            to = webmaster;
        }
        if (enabled) {
            try {
                log.info("Queue sending mail to: '{}', subject: '{}' ", to, subject);

                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED);
                message.setFrom(from);
                helper.setTo(parse(to));
                if (!Strings.isNullOrEmpty(replyTo))
                    helper.setReplyTo(parse(replyTo));
                if (!Strings.isNullOrEmpty(bcc))
                    helper.setBcc(parse(bcc));
                helper.setSubject(subject);
                helper.setText(text, true);

                messageQueue.add(message);
            }
            catch (MessagingException e) {
                log.error("Failed to send mail to: '{}', subject: '{}'", to, subject, e);
            }
        }
        else {
            log.info("Skip sending mail to: '{}', subject: '{}', text: '{}'", to, subject, text);
        }
    }

    @Scheduled(fixedDelay = 10*1000)
    protected void processQueue() {
        if (!messageQueue.isEmpty()) {
            MimeMessage message = messageQueue.poll();
            try {
                log.info("Sending queued message to: '{}', subject: '{}'", message.getAllRecipients(), message.getSubject());
                mailSender.send(message);
            }
            catch (MailException | MessagingException e) {
                log.error("Failed to send mails.", e);
            }
        }
    }
}
