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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class WeeklySummaryService {

    @NonNull
    private final PersonRepository personRepository;
    @NonNull
    private final CheckinRepository checkinRepository;
    @NonNull
    private final MailService mailService;

    @Value("${checkin.mail.from}")
    private String from;
    @Value("${checkin.mail.to}")
    private String to;

    @Scheduled(cron = "0 0 8 * * SUN") //every sunday at 08:00
    public void sendWeekyMail() {

        LocalDateTime startOfToday = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime startOfLastWeek = startOfToday.minusDays(7);

        StringBuilder overallSummaryMessageBuilder = new StringBuilder();
        overallSummaryMessageBuilder.append("Weeky Summary").append("\n");

        for (Person person : personRepository.findAll()) {

            Duration total = Duration.ZERO;
            List<Checkin> checkins = checkinRepository.findByPersonAndCheckedInTrueOrderByTime(person);
            for (Checkin checkin : checkins) {
                if (checkin.getTime().isAfter(startOfLastWeek) && !checkin.getTime().isAfter(startOfToday))
                    total = total.plus(checkin.getDuration());
            }

            String personalMessage = String.format("Hello %s! You have been checked in for %s since %s until %s.",
                    person.getName(),
                    formatDuration(total),
                    startOfLastWeek.format(DateTimeFormatter.ISO_DATE_TIME),
                    startOfToday.format(DateTimeFormatter.ISO_DATE_TIME)
            );

            String summaryMessage = String.format("%s has been checked in for %s",
                    person.getName(),
                    formatDuration(total)
            );

            overallSummaryMessageBuilder.append(summaryMessage).append("\n");

            //send mail to user with summary of hours during the last week
            if (person.getEmail() != null) {
                mailService.sendMail(from, person.getEmail(), null, null,
                        "Your RefugeesCode Check-in Weekly Summary",
                        personalMessage);
            }
        }

        overallSummaryMessageBuilder.append(String.format("since %s until %s.",
                startOfLastWeek.format(DateTimeFormatter.ISO_DATE_TIME),
                startOfToday.format(DateTimeFormatter.ISO_DATE_TIME)
        ));

        String overallSummaryMessage = overallSummaryMessageBuilder.toString();

        log.info("{}", overallSummaryMessage);

        //send mail to admin with summary of hours during the last week for all users
        mailService.sendMail(from, to, null, null, "RefugeesCode Check-in Summary", overallSummaryMessage);

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
