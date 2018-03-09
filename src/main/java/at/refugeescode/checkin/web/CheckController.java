package at.refugeescode.checkin.web;

import at.refugeescode.checkin.config.SlackAppender;
import at.refugeescode.checkin.domain.*;
import at.refugeescode.checkin.dto.Attendance;
import at.refugeescode.checkin.dto.Overview;
import at.refugeescode.checkin.dto.Summary;
import at.refugeescode.checkin.service.CheckService;
import at.refugeescode.checkin.service.OverviewService;
import at.refugeescode.checkin.service.PersonService;
import com.google.common.collect.Iterables;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.jcache.JCacheCache;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class CheckController {

    @NonNull
    private final PersonRepository personRepository;
    @NonNull
    private final PersonService personService;
    @NonNull
    private final CheckService checkService;
    @NonNull
    private final OverviewService overviewService;
    @NonNull
    private final CheckRepository checkRepository;
    @NonNull
    private final ProjectionFactory projectionFactory;
    @NonNull
    private final CacheManager cacheManager;

    @GetMapping("/hello")
    public ResponseEntity<Void> hello() {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login() {
        if (SecurityContextHolder.getContext().getAuthentication().isAuthenticated())
            return new ResponseEntity<>(HttpStatus.OK);
        else
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @GetMapping("/people/{uid}/checkin")
    public ResponseEntity<CheckStatusProjection> newCheck(@PathVariable("uid") String uid) {

        Check check = checkService.newCheck(uid, false);

        log.info(SlackAppender.POST_TO_SLACK, "{} has checked {} at {}",
                "User '" + check.getPerson().getName() + "'",
                check.isCheckedIn() ? "in" : "out",
                DateTimeFormatter.ofPattern("dd.MM.YYYY HH:mm").format(check.getTime())
        );

        CheckStatusProjection projection = projectionFactory.createProjection(CheckStatusProjection.class, check);
        return new ResponseEntity<>(projection, HttpStatus.OK);
    }

    @GetMapping("/people/{uid}/status")
    public ResponseEntity<Boolean> status(@PathVariable("uid") String uid) {

        Person person = personRepository.findByUid(uid);
        if (person == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(checkService.isCheckedIn(person), HttpStatus.OK);
    }

    @PutMapping("/people/{uid}/toggle")
    public ResponseEntity<Person> toggle(@PathVariable("uid") String uid) {

        Person person = personRepository.findByUid(uid);
        if (person == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        person.setDisabled(!person.isDisabled());
        person = personRepository.save(person);

        return new ResponseEntity<>(person, HttpStatus.OK);
    }

    @DeleteMapping("/people/{uid}/delete")
    @Transactional(readOnly = false)
    public ResponseEntity<Void> delete(@PathVariable("uid") String uid) {

        Person person = personRepository.findByUid(uid);
        if (person == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        personService.delete(person);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/overview/{yearMonth}")
    @Transactional(readOnly = true)
    public ResponseEntity<Overview> overview(@PathVariable("yearMonth") YearMonth yearMonth) {

        List<Person> people = personService.findEnabledUsers();
        List<String> columns = overviewService.getOverviewColumns(yearMonth);
        List<String> avgCheckOutTimes = overviewService.getOverviewAvgCheckOutTimes(yearMonth);

        List<Attendance> attendances = new ArrayList<>();
        for (Person person : people)
            attendances.add(new Attendance(person.getName(), overviewService.getOverviewDurations(yearMonth, person)));

        return new ResponseEntity<>(new Overview(yearMonth, columns, attendances, avgCheckOutTimes), HttpStatus.OK);
    }

    @GetMapping("/checks/{uid}/{yearMonth}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<CheckLogProjection>> checksByPersonAndMonth(
            @PathVariable("uid") String uid,
            @PathVariable("yearMonth") YearMonth yearMonth) {

        Person person = personRepository.findByUid(uid);
        if (person == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        List<Check> checks = checkRepository.findByPersonAndTimeBetweenOrderByTimeDesc(
                person,
                yearMonth.atDay(1).atStartOfDay(),
                yearMonth.atEndOfMonth().plusDays(1).atStartOfDay());

        return new ResponseEntity<>(createProjectionList(CheckLogProjection.class, checks), HttpStatus.OK);
    }

    @GetMapping("/avg-check-out-time/{date}")
    @Transactional(readOnly = true)
    public ResponseEntity<String> avgCheckOutTime(@PathVariable("date") LocalDate date) {
        String avgCheckOutTime = checkService.getAvgCheckOutTime(date).format(DateTimeFormatter.ofPattern("HH:mm"));
        return new ResponseEntity<>(avgCheckOutTime, HttpStatus.OK);
    }

    @GetMapping("/client/summary")
    @Transactional(readOnly = true)
    public ResponseEntity<List<PersonStatusDetailProjection>> clientSummary() {
        List<Person> all = personRepository.findAll();
        List<PersonStatusDetailProjection> personStatusList = createProjectionList(PersonStatusDetailProjection.class, all);
        return new ResponseEntity<>(personStatusList, HttpStatus.OK);
    }

    @GetMapping("/public/summary")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Summary>> publicSummary() {
        List<Person> enabledUsers = personService.findEnabledUsers();
        List<Summary> summaries = enabledUsers.stream()
                .map(overviewService::getSummary)
                .collect(Collectors.toList());
        return new ResponseEntity<>(summaries, HttpStatus.OK);
    }

    private <T> List<T> createProjectionList(Class<T> projectionType, List<?> sourceList) {
        return sourceList.stream()
                .map(source -> projectionFactory.createProjection(projectionType, source))
                .collect(Collectors.toList());
    }

    @GetMapping(value = "/dump-caches")
    public ResponseEntity<Map<String, Map<Object, Object>>> dumpCaches() {
        Map<String, Map<Object, Object>> result = new HashMap<>();
        for (String cacheName : cacheManager.getCacheNames()) {
            log.trace("Cache: {}", cacheName);
            Cache cache = cacheManager.getCache(cacheName);
            JCacheCache jCacheCache = (JCacheCache) cache;
            javax.cache.Cache<Object, Object> nativeCache = jCacheCache.getNativeCache();
            Map<Object, Object> cacheMap = new HashMap<>();
            for (javax.cache.Cache.Entry<Object, Object> entry : nativeCache) {
                log.trace("{}: {}", entry.getKey(), entry.getValue());
                cacheMap.put(entry.getKey(), entry.getValue());
            }
            result.put(cacheName, cacheMap);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

}
