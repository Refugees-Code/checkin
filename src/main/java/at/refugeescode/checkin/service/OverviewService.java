package at.refugeescode.checkin.service;

import at.refugeescode.checkin.domain.Check;
import at.refugeescode.checkin.domain.CheckRepository;
import at.refugeescode.checkin.domain.Person;
import at.refugeescode.checkin.dto.Summary;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
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
public class OverviewService {

    private static final String OVERVIEW_WEEK_COLUMN_PREFIX = "w-";
    private static final String OVERVIEW_ESTIMATED_PREFIX = "~";

    @NonNull
    private final CheckService checkService;

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

        List<String> durations = new ArrayList<>(yearMonth.lengthOfMonth()+4);

        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate startOfNextMonth = yearMonth.plusMonths(1).atDay(1);

        Duration weekTotal = Duration.ZERO;
        boolean weekEstimated = false;

        for (LocalDate day = startOfMonth; day.isBefore(startOfNextMonth); day = day.plusDays(1)) {

            Pair<Duration, Boolean> dayDurationPair = checkService.getDayDuration(person, day);
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
    public List<String> getOverviewAvgCheckOutTimes(YearMonth yearMonth) {

        List<String> avgCheckOutTimes = new ArrayList<>(yearMonth.lengthOfMonth()+4);

        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate startOfNextMonth = yearMonth.plusMonths(1).atDay(1);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        for (LocalDate day = startOfMonth; day.isBefore(startOfNextMonth); day = day.plusDays(1)) {
            avgCheckOutTimes.add(checkService.getAvgCheckOutTime(day).format(timeFormatter));
            if (day.getDayOfWeek() == DayOfWeek.SUNDAY) {
                avgCheckOutTimes.add(null);
            }
        }

        return avgCheckOutTimes;
    }

    @Transactional(readOnly = true)
    public Duration getLastWeekDuration(Person person) {

        LocalDate today = LocalDate.now();
        LocalDate previousSunday = today.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));

        Duration weekTotal = Duration.ZERO;

        for (LocalDate day = previousSunday; !day.isAfter(today); day = day.plusDays(1)) {
            weekTotal = weekTotal.plus(checkService.getDayDuration(person, day).getFirst());
        }

        return weekTotal;
    }


    @Transactional(readOnly = true)
    public Summary getSummary(Person person) {
        String name = person.getName();
        LocalDateTime now = LocalDateTime.now();

        Optional<Check> lastCheck = checkService.lastCheck(person);
        Optional<LocalDateTime> lastTime = lastCheck.map(Check::getTime);

        boolean checkedIn = lastCheck.map(Check::isCheckedIn).orElse(false);
        boolean auto = lastCheck.map(Check::isAuto).orElse(false);

        Duration lastDuration = lastTime.isPresent() ? Duration.between(lastTime.get(), now) : null;
        Duration weekDuration = getLastWeekDuration(person);

        return new Summary(name, checkedIn, auto, lastDuration, weekDuration);
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
}
