package at.refugeescode.checkin.web;

import at.refugeescode.checkin.config.SlackAppender;
import at.refugeescode.checkin.domain.*;
import at.refugeescode.checkin.dto.Attendance;
import at.refugeescode.checkin.dto.Overview;
import at.refugeescode.checkin.service.CheckinService;
import at.refugeescode.checkin.service.PersonService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class CheckinController {

    @NonNull
    private final PersonRepository personRepository;
    @NonNull
    private final PersonService personService;
    @NonNull
    private final CheckinService checkinService;
    @NonNull
    private final CheckinRepository checkinRepository;
    @NonNull
    private final ProjectionFactory projectionFactory;

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
    @Transactional(readOnly = false)
    public ResponseEntity<Checkin> checkin(@PathVariable("uid") String uid) {

        Checkin checkin = checkinService.newCheck(uid, false);

        log.info(SlackAppender.POST_TO_SLACK, "{} has checked {} at {}",
                "User '" + checkin.getPerson().getName() + "'",
                checkin.isCheckedIn() ? "in" : "out",
                DateTimeFormatter.ofPattern("dd.MM.YYYY HH:mm").format(checkin.getTime())
        );

        return new ResponseEntity<>(checkin, HttpStatus.OK);
    }

    @GetMapping("/people/{uid}/status")
    @Transactional(readOnly = true)
    public ResponseEntity<Boolean> status(@PathVariable("uid") String uid) {

        Person person = personRepository.findByUid(uid);
        if (person == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(checkinService.isCheckedIn(person), HttpStatus.OK);
    }

    @PutMapping("/people/{uid}/toggle")
    @Transactional(readOnly = false)
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

        List<Checkin> checkins = checkinRepository.findByPerson(person);
        checkinRepository.delete(checkins);

        personRepository.delete(person);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/overview/{yearMonth}")
    @Transactional(readOnly = true)
    public ResponseEntity<Overview> overview(@PathVariable("yearMonth") YearMonth yearMonth) {

        List<Person> people = personService.findEnabledNonNewUsers();
        List<String> columns = checkinService.getOverviewColumns(yearMonth);
        List<String> avgCheckOutTimes = checkinService.getOverviewAvgCheckOutTimes(yearMonth);

        List<Attendance> attendances = new ArrayList<>();
        for (Person person : people)
            attendances.add(new Attendance(person.getName(), checkinService.getOverviewDurations(yearMonth, person)));

        return new ResponseEntity<>(new Overview(yearMonth, columns, attendances, avgCheckOutTimes), HttpStatus.OK);
    }

    @GetMapping("/checks/{uid}/{date}")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Checkin>> checksByPersonAndDate(
            @PathVariable("uid") String uid,
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Person person = personRepository.findByUid(uid);
        if (person == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        List<Checkin> checks = checkinRepository.findByPersonAndTimeBetweenOrderByTimeDesc(person, date.atStartOfDay(), date.plusDays(1).atStartOfDay());

        return new ResponseEntity<>(checks, HttpStatus.OK);
    }

    @PutMapping("/update-time/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<Checkin> updateTime(
            @PathVariable("id") Long id,
            @RequestParam("time")  @DateTimeFormat(pattern = "HH:mm") LocalTime time) {

        Checkin check = checkinRepository.findOne(id);
        if (check == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        check.setTime(check.getTime().withHour(time.getHour()).withMinute(time.getMinute()));
        check = checkinRepository.save(check);

        return new ResponseEntity<>(check, HttpStatus.OK);
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
    public ResponseEntity<List<PersonStatusProjection>> publicSummary() {
        List<Person> nonNewUsers = personService.findEnabledNonNewUsers();
        List<PersonStatusProjection> personStatusList = createProjectionList(PersonStatusProjection.class, nonNewUsers);
        return new ResponseEntity<>(personStatusList, HttpStatus.OK);
    }

    private <T> List<T> createProjectionList(Class<T> projectionType, List<?> sourceList) {
        return sourceList.stream()
                .map(source -> projectionFactory.createProjection(projectionType, source))
                .collect(Collectors.toList());
    }

}
