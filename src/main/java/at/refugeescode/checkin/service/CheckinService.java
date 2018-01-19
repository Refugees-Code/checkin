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
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class CheckinService {

    private static final String FORGOT_CHECK_OUT_MESSAGE = "Hello %s!<br/><br/>" +
            "Did you forget to check out today?<br/></br/>" +
            "You have been automatically checked out at midnight.<br/></br/>" +
            "Your refugees{code}-Team";

    private static final String OVERVIEW_WEEK_COLUMN_PREFIX = "w-";
    private static final String OVERVIEW_ESTIMATED_PREFIX = "~";

    @NonNull
    private final CheckinRepository checkinRepository;
    @NonNull
    private final PersonRepository personRepository;
    @NonNull
    private final MailService mailService;

    @Value("${checkin.mail.webmaster}")
    private String webmaster;

    @Transactional(readOnly = true)
    public Optional<Checkin> lastCheck(Person person) {
        return checkinRepository.findFirstByPersonOrderByTimeDesc(person);
    }

    @Transactional(readOnly = true)
    public Optional<Checkin> lastCheckBefore(Person person, LocalDateTime end) {
        return checkinRepository.findFirstByPersonAndTimeBeforeOrderByTimeDesc(person, end);
    }

    @Transactional(readOnly = true)
    public boolean isCheckedIn(Person person) {
        return lastCheck(person).map(Checkin::isCheckedIn).orElse(false);
    }

    @Transactional(readOnly = true)
    public List<String> getOverviewColumns(YearMonth yearMonth) {

        List<String> columns = new ArrayList<>(yearMonth.lengthOfMonth());

        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate startOfNextMonth = yearMonth.plusMonths(1).atDay(1);

        int week = 0;

        for (LocalDate day = startOfMonth; day.isBefore(startOfNextMonth); day = day.plusDays(1)) {

            columns.add(String.valueOf(day.getDayOfMonth()));

            if (day.getDayOfWeek() == DayOfWeek.SUNDAY)
                columns.add(OVERVIEW_WEEK_COLUMN_PREFIX + (++week));
        }

        return columns;
    }

    @Transactional(readOnly = true)
    public List<String> getOverviewDurations(YearMonth yearMonth, Person person) {

        List<String> durations = new ArrayList<>(yearMonth.lengthOfMonth());

        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate startOfNextMonth = yearMonth.plusMonths(1).atDay(1);

        Duration weekTotal = Duration.ZERO;
        boolean weekEstimated = false;

        for (LocalDate day = startOfMonth; day.isBefore(startOfNextMonth); day = day.plusDays(1)) {

            Pair<Duration, Boolean> dayDurationPair = getDayDuration(person, day);
            Duration dayDuration = dayDurationPair.getFirst();
            boolean estimated = dayDurationPair.getSecond();

            durations.add(formatDuration(dayDuration, estimated));

            weekTotal = weekTotal.plus(dayDuration);
            weekEstimated = weekEstimated || estimated;

            if (day.getDayOfWeek() == DayOfWeek.SUNDAY) {
                durations.add(formatDuration(weekTotal, weekEstimated));
                weekTotal = Duration.ZERO;
                weekEstimated = false;
            }
        }

        return durations;
    }

    @Transactional(readOnly = true)
    public Duration getLastWeekDuration(Person person) {

        LocalDate today = LocalDate.now();
        LocalDate previousSunday = today.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));

        Duration weekTotal = Duration.ZERO;

        for (LocalDate day = previousSunday; !day.isAfter(today); day = day.plusDays(1)) {
            weekTotal = weekTotal.plus(getDayDuration(person, day).getFirst());
        }

        return weekTotal;
    }

    public Pair<Duration, Boolean> getDayDuration(Person person, LocalDate day) {
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime startOfNextDay = day.plusDays(1).atStartOfDay();

        List<Checkin> checkins = checkinRepository.findByPersonAndCheckedInFalseAndTimeBetweenOrderByTimeDesc(person, startOfDay, startOfNextDay);

        Duration duration = Duration.ZERO;
        boolean estimated = false;
        for (Checkin checkin : checkins) {
            duration = duration.plus(checkin.getDuration());
            if (checkin.isAuto())
                estimated = true;
        }

        return Pair.of(duration, estimated);
    }

    public static long ceilMinutes(Duration duration) {
        if (duration.getSeconds() % 60 != 0 || duration.getNano() != 0)
            return duration.toMinutes() + 1;
        else
            return duration.toMinutes();
    }

    public static String formatDuration(Duration duration) {
        return duration.isZero() ? "" : String.format("%.1f", ceilMinutes(duration) / 60.0);
    }

    public static String formatDuration(Duration duration, boolean estimated) {
        return (estimated ? OVERVIEW_ESTIMATED_PREFIX : "") + formatDuration(duration);
    }

    @Transactional(readOnly = true)
    public Duration getLastCheckInTime(Person person) {
        LocalDateTime now = LocalDateTime.now();
        Optional<LocalDateTime> lastTime = lastCheck(person).map(Checkin::getTime);
        return lastTime.isPresent() ? Duration.between(lastTime.get(), now) : null;
    }

    @Transactional(readOnly = false)
    public Checkin newCheck(String uid, boolean auto) {
        Person person = personRepository.findByUid(uid);

        if (person == null) {
            String placeholder = "new-user-" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
            person = personRepository.save(new Person(uid, placeholder, placeholder));
        }

        Optional<Checkin> lastCheckOptional = lastCheck(person);
        LocalDateTime now = LocalDateTime.now();
        Checkin check;

        if (!lastCheckOptional.isPresent()) {
            check = new Checkin(person, now, Duration.ZERO, true, auto);
        }
        else {
            Checkin lastCheck = lastCheckOptional.get();
            Duration duration = Duration.between(lastCheck.getTime(), now);
            check = new Checkin(person, now, duration, !lastCheck.isCheckedIn(), auto);
        }

        check = checkinRepository.save(check);

        return check;
    }

    @Scheduled(cron = "${checkin.autoCheckOut}")
    @Transactional(readOnly = false)
    public void autoCheckOut() {
        List<Person> people = personRepository.findAll();
        for (Person person : people) {
            if (isCheckedIn(person)) {
                newCheck(person.getUid(), true);

                mailService.sendMail(person, null, webmaster,
                        "RefugeesCode Attendance - Forgot to check out?",
                        String.format(FORGOT_CHECK_OUT_MESSAGE, person.getName()));
            }
        }
    }
}
