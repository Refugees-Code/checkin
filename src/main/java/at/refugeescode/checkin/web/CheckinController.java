package at.refugeescode.checkin.web;

import at.refugeescode.checkin.config.SlackAppender;
import at.refugeescode.checkin.domain.*;
import at.refugeescode.checkin.service.CheckinService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class CheckinController {

    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.YYYY HH:mm");

    @NonNull
    private final PersonRepository personRepository;
    @NonNull
    private final CheckinRepository checkinRepository;
    @NonNull
    private final CheckinService checkinService;
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
    @Transactional
    public ResponseEntity<Boolean> checkin(@PathVariable("uid") String uid) {

        Person person = personRepository.findByUid(uid);

        if (person == null) {
            String placeholder = "new-user-" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
            person = personRepository.save(new Person(uid, placeholder, placeholder));
        }

        Optional<Checkin> lastCheckOptional = checkinService.lastCheck(person);
        LocalDateTime now = LocalDateTime.now();
        Checkin checkin;

        if (!lastCheckOptional.isPresent()) {
            checkin = new Checkin(person, now, Duration.ZERO, true);
        }
        else {
            Checkin lastCheckin = lastCheckOptional.get();
            Duration duration = Duration.between(lastCheckin.getTime(), now);
            checkin = new Checkin(person, now, duration, !lastCheckin.isCheckedIn());
        }

        checkin = checkinRepository.save(checkin);

        log.info(SlackAppender.POST_TO_SLACK, "{} has checked {} at {}",
                "User '" + person.getName() + "'",
                checkin.isCheckedIn() ? "in" : "out",
                now.format(dateTimeFormatter)
        );

        return new ResponseEntity<>(checkin.isCheckedIn(), HttpStatus.OK);
    }

    @GetMapping("/people/{uid}/status")
    @Transactional
    public ResponseEntity<Boolean> status(@PathVariable("uid") String uid) {

        Person person = personRepository.findByUid(uid);

        if (person == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        return new ResponseEntity<>(checkinService.isCheckedIn(person), HttpStatus.OK);
    }

    @GetMapping("/public/summary")
    @Transactional
    public ResponseEntity<List<PersonStatusProjection>> summary() {
        return new ResponseEntity<>(createProjectionList(PersonStatusProjection.class, personRepository.findAll()), HttpStatus.OK);
    }

    private <T> List<T> createProjectionList(Class<T> projectionType, List<?> sourceList) {
        return sourceList.stream()
                .map(source -> projectionFactory.createProjection(projectionType, source))
                .collect(Collectors.toList());
    }

}
