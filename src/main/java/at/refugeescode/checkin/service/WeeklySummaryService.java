package at.refugeescode.checkin.service;

import at.refugeescode.checkin.domain.Checkin;
import at.refugeescode.checkin.domain.CheckinRepository;
import at.refugeescode.checkin.domain.Person;
import at.refugeescode.checkin.domain.PersonRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class WeeklySummaryService {

    private static final String PERSONAL_MESSAGE = "Hello %s!<br/><br/>" +
            "Another week has passed and we're happy to share with you how much time you were present!<br/></br/>" +
            "During last week, from %s until %s, you have been checked in for %s.<br/></br/>" +
            "Happy coding and see you next week!<br/><br/>" +
            "Your refugees{code}-Team";
    private static final String SUMMARY_MESSAGE = "Hello Trainer!<br/><br/>" +
            "Here's the summary for the week from %s until %s:" +
            "<table>" +
            "%s" +
            "</table>" +
            "<br/><br/>" +
            "Happy coding!";
    private static final String SUMMARY_ROW = "<tr><td>%s</td><td>%s</td></tr>";

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

    @Scheduled(cron = "${checkin.mail.weekly}")
    public void sendWeeklyMail() {
        log.info("Sending weekly mails");

        LocalDate previousOrSameSunday = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDateTime startOfToday = previousOrSameSunday.atStartOfDay();
        LocalDateTime startOfLastWeek = previousOrSameSunday.minusDays(7).atStartOfDay();

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("EEEE, dd.MM.yyyy");
        String formattedStartOfToday = dateFormatter.format(previousOrSameSunday.minusDays(1));
        String formattedStartOfLastWeek = dateFormatter.format(previousOrSameSunday.minusDays(7));

        StringBuilder rowMessageBuilder = new StringBuilder();

        for (Person person : personRepository.findAllByOrderByName()) {

            Duration total = Duration.ZERO;
            List<Checkin> checkins = checkinRepository.findByPersonAndCheckedInFalseOrderByTime(person);
            for (Checkin checkin : checkins) {
                if (checkin.getTime().isAfter(startOfLastWeek) && !checkin.getTime().isAfter(startOfToday))
                    total = total.plus(checkin.getDuration());
            }

            rowMessageBuilder.append(String.format(SUMMARY_ROW,
                    person.getName(),
                    formatDuration(total, "%d:%02d")
            ));

            String personalMessage = String.format(PERSONAL_MESSAGE,
                    person.getName(),
                    formattedStartOfLastWeek,
                    formattedStartOfToday,
                    formatDuration(total, "%d hours and %d minutes")
            );

            //send mail to user with summary of hours during the last week
            mailService.sendMail(person, null, webmaster,
                    "Your RefugeesCode Weekly Attendance Summary",
                    personalMessage);
        }

        String overallSummaryMessage = String.format(SUMMARY_MESSAGE,
                formattedStartOfLastWeek,
                formattedStartOfToday,
                rowMessageBuilder.toString());

        //send mail to admin with summary of hours during the last week for all users
        mailService.sendMail(trainer, null, null, "RefugeesCode Attendance Summary", overallSummaryMessage);
    }

    private static long ceilMinutes(Duration duration) {
        if (duration.getSeconds() % 60 != 0 || duration.getNano() != 0)
            return duration.toMinutes() + 1;
        else
            return duration.toMinutes();
    }

    private static String formatDuration(Duration duration, String format) {
        duration = Duration.ofMinutes(ceilMinutes(duration));
        long hoursPart = duration.toHours();
        long minutesPart = duration.minusHours(hoursPart).toMinutes();
        return String.format(format, hoursPart, minutesPart);
    }
}
