package at.refugeescode.checkin.service;

import at.refugeescode.checkin.domain.Checkin;
import at.refugeescode.checkin.domain.CheckinRepository;
import at.refugeescode.checkin.domain.Person;
import at.refugeescode.checkin.domain.PersonRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class WeeklySummaryService {

    private static final String PERSONAL_MESSAGE = "Hello %s!<br/><br/>" +
            "Another week has passed and we're happy to share with you how much time you were present!<br/>" +
            "You have been checked in for %s hours, in the week from %s until %s.<br/>" +
            "Happy coding and see you next week!<br/><br/>" +
            "Your refugees{code}-Team";
    private static final String SUMMARY_MESSAGE = "Hello Trainer!<br/><br/>" +
            "Here's the summary for the week from %s until %s:" +
            "<table>" +
            "%s" +
            "</table>" +
            "<br/><br/>" +
            "Happy coding!";

    @NonNull
    private final PersonRepository personRepository;
    @NonNull
    private final CheckinRepository checkinRepository;
    @NonNull
    private final MailService mailService;

    @Value("${checkin.mail.trainer}")
    private String trainer;
    @Value("${checkin.mail.webmaster}")
    private String webmaster;
    @Value("${checkin.mail.weekly}")
    private String weekly;

    private static final EmailValidator emailValidator = new EmailValidator();

    @Scheduled(cron = "${checkin.mail.weekly}")
    public void sendWeekyMail() {
        log.info("Sending weekly mails");

        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfLastWeek = today.minusDays(7).atStartOfDay();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy");
        String formattedStartOfToday = dateFormatter.format(today);
        String formattedStartOfLastWeek = dateFormatter.format(today.minusDays(6));

        StringBuilder rowMessageBuilder = new StringBuilder();

        for (Person person : personRepository.findAll()) {

            Duration total = Duration.ZERO;
            List<Checkin> checkins = checkinRepository.findByPersonAndCheckedInTrueOrderByTime(person);
            for (Checkin checkin : checkins) {
                if (checkin.getTime().isAfter(startOfLastWeek) && !checkin.getTime().isAfter(startOfToday))
                    total = total.plus(checkin.getDuration());
            }

            rowMessageBuilder.append(String.format("<tr><td>%s</td><td>%s</td></tr>",
                    person.getName(),
                    formatDuration(total)
            ));

            String personalMessage = String.format(PERSONAL_MESSAGE,
                    person.getName(),
                    formatDuration(total),
                    formattedStartOfLastWeek,
                    formattedStartOfToday
            );

            //send mail to user with summary of hours during the last week
            if (person.getEmail() != null && emailValidator.isValid(person.getEmail(), null)) {
                mailService.sendMail(person.getEmail(), null, webmaster,
                        "Your RefugeesCode Check-in Weekly Summary",
                        personalMessage);
            }
        }

        String overallSummaryMessage = String.format(SUMMARY_MESSAGE,
                formattedStartOfLastWeek,
                formattedStartOfToday,
                rowMessageBuilder.toString());

        //send mail to admin with summary of hours during the last week for all users
        mailService.sendMail(trainer, null, null, "RefugeesCode Check-in Summary", overallSummaryMessage);
    }

    private static long ceilMinutes(Duration duration) {
        if (duration.getSeconds() % 60 != 0 || duration.getNano() != 0)
            return duration.toMinutes() + 1;
        else
            return duration.toMinutes();
    }

    private static String formatDuration(Duration duration) {
        duration = Duration.ofMinutes(ceilMinutes(duration));
        long hoursPart = duration.toHours();
        long minutesPart = duration.minusHours(hoursPart).toMinutes();
        return String.format("%d:%02d", hoursPart, minutesPart);
    }
}
