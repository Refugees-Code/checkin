package at.refugeescode.checkin.service;

import at.refugeescode.checkin.domain.Check;
import at.refugeescode.checkin.domain.CheckRepository;
import at.refugeescode.checkin.domain.Person;
import at.refugeescode.checkin.domain.PersonRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.util.Pair;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class CheckService {

    private static final String FORGOT_CHECK_OUT_SUBJECT = "RefugeesCode Attendance - Forgot to check out?";
    private static final String FORGOT_CHECK_OUT_MESSAGE = "Hello %s!<br/><br/>" +
            "Did you forget to check out today?<br/></br/>" +
            "You have been automatically checked out at midnight.<br/></br/>" +
            "Your refugees{code}-Team";

    private static final String NEW_USER_PREFIX = "new-user-";

    @NonNull
    private final CheckRepository checkRepository;
    @NonNull
    private final PersonRepository personRepository;
    @NonNull
    private final MailService mailService;

    @Value("${checkin.mail.webmaster}")
    private String webmaster;

    @Transactional(readOnly = true)
    public Optional<Check> lastCheck(Person person) {
        return checkRepository.findFirstByPersonOrderByTimeDesc(person);
    }

    @Transactional(readOnly = true)
    public Optional<Check> lastCheckBefore(Person person, LocalDateTime end) {
        return checkRepository.findFirstByPersonAndTimeBeforeOrderByTimeDesc(person, end);
    }

    @Transactional(readOnly = true)
    public Optional<Check> lastCheckOut(Person person, LocalDateTime start, LocalDateTime end) {
        return checkRepository.findFirstByCheckedInFalseAndPersonAndTimeBetweenOrderByTimeDesc(person, start, end);
    }

    @Transactional(readOnly = true)
    public boolean isCheckedIn(Person person) {
        return lastCheck(person).map(Check::isCheckedIn).orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean isAuto(Person person) {
        return lastCheck(person).map(Check::isAuto).orElse(false);
    }

    @Transactional(readOnly = true)
    public Duration getDuration(Check check) {
        LocalDateTime time = check.getTime();
        Optional<Check> before = lastCheckBefore(check.getPerson(), time);
        return before.isPresent() ? Duration.between(before.get().getTime(), time) : Duration.ZERO;
    }

    @Transactional(readOnly = true)
    public Duration getEstimatedDuration(Check check, LocalTime avgCheckOutTime) {
        LocalDateTime time = check.getTime();
        LocalDateTime avgCheckOutTimeToday = avgCheckOutTime.atDate(time.toLocalDate());
        Optional<Check> before = lastCheckBefore(check.getPerson(), time);
        return before.isPresent() ? Duration.between(before.get().getTime(), avgCheckOutTimeToday) : Duration.ZERO;
    }

    @Transactional(readOnly = true)
    public LocalTime getAvgCheckOutTime(LocalDate day) {
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime startOfNextDay = day.plusDays(1).atStartOfDay();

        List<Person> enabledUsers = personRepository.findByDisabledFalse();
        List<Check> lastCheckOuts = new ArrayList<>(enabledUsers.size());
        for (Person person : enabledUsers) {
            Optional<Check> lastCheckOut = lastCheckOut(person, startOfDay, startOfNextDay);
            lastCheckOut.ifPresent(lastCheckOuts::add);
        }

        OptionalDouble avgSecondsOfDayOptional = lastCheckOuts.stream()
                .mapToInt(check -> check.getTime().toLocalTime().toSecondOfDay())
                .average();

        LocalTime avgCheckOutTime;
        if (avgSecondsOfDayOptional.isPresent()) {
            double avgSecondsOfDay = avgSecondsOfDayOptional.getAsDouble();
            avgCheckOutTime = LocalTime.ofSecondOfDay((long) avgSecondsOfDay);
        }
        else
            avgCheckOutTime = LocalTime.MAX;

        return avgCheckOutTime;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = {"dayDurations"}, key = "{ #person.uid, #day }")
    public Pair<Duration, Boolean> getDayDuration(Person person, LocalDate day) {
        LocalDateTime startOfDay = day.atStartOfDay();
        LocalDateTime startOfNextDay = day.plusDays(1).atStartOfDay();

        List<Check> checks = checkRepository.findByPersonAndCheckedInFalseAndTimeBetweenOrderByTimeDesc(person, startOfDay, startOfNextDay);
        LocalTime avgCheckOutTime = getAvgCheckOutTime(day);

        Duration totalDuration = Duration.ZERO;
        boolean estimated = false;
        for (Check check : checks) {
            Duration duration;
            if (check.isAuto()) {
                estimated = true;
                duration = getEstimatedDuration(check, avgCheckOutTime);
            }
            else {
                duration = getDuration(check);
            }
            totalDuration = totalDuration.plus(duration);
        }

        return Pair.of(totalDuration, estimated);
    }



    @Transactional(readOnly = true)
    public Duration getLastCheckInTime(Person person) {
        LocalDateTime now = LocalDateTime.now();
        Optional<LocalDateTime> lastTime = lastCheck(person).map(Check::getTime);
        return lastTime.isPresent() ? Duration.between(lastTime.get(), now) : null;
    }

    @Transactional(readOnly = false)
    @CacheEvict(cacheNames = {"dayDurations"}, key = "{ #uid, T(java.time.LocalDate).now() }")
    public Check newCheck(String uid, boolean auto) {
        Person person = personRepository.findByUid(uid);

        if (person == null) {
            String placeholder = NEW_USER_PREFIX + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
            Person newUser = new Person(uid, placeholder, placeholder);
            newUser.setDisabled(true);
            person = personRepository.save(newUser);
        }

        Optional<Check> lastCheck = lastCheck(person);
        LocalDateTime now = LocalDateTime.now();
        boolean checkedIn = lastCheck.map(check -> !check.isCheckedIn()).orElse(true);

        Check check = new Check(person, now, checkedIn, auto);
        check = checkRepository.save(check);
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
                        FORGOT_CHECK_OUT_SUBJECT,
                        String.format(FORGOT_CHECK_OUT_MESSAGE, person.getName()));
            }
        }
    }
}
