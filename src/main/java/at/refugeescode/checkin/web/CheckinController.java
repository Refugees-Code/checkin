package at.refugeescode.checkin.web;

import at.refugeescode.checkin.config.SlackAppender;
import at.refugeescode.checkin.domain.Checkin;
import at.refugeescode.checkin.domain.CheckinRepository;
import at.refugeescode.checkin.domain.Person;
import at.refugeescode.checkin.domain.PersonRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@RestController
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class CheckinController {

    public static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.YYYY HH:mm");

    @NonNull
    private final PersonRepository personRepository;
    @NonNull
    private final CheckinRepository checkinRepository;

    private boolean isCheckedIn(Person person) {
        Optional<Checkin> lastCheckinOptional = checkinRepository.findLastByPersonOrderByTime(person);
        return lastCheckinOptional.isPresent() && lastCheckinOptional.get().isCheckedIn();
    }

    @GetMapping("/people/{uid}/checkin")
    @Transactional
    public ResponseEntity<Boolean> checkin(@PathVariable("uid") String uid) {

        Person person = personRepository.findByUid(uid);

        if (person == null) {
            person = personRepository.save(new Person(uid));
        }

        Optional<Checkin> lastCheckinOptional = checkinRepository.findLastByPersonOrderByTime(person);
        LocalDateTime now = LocalDateTime.now();
        Checkin checkin;

        if (lastCheckinOptional.isPresent()) {
            Checkin lastCheckin = lastCheckinOptional.get();
            Duration duration = Duration.between(lastCheckin.getTime(), now);
            checkin = new Checkin(person, now, duration, !lastCheckin.isCheckedIn());
        }
        else {
            checkin = new Checkin(person, now, Duration.ZERO, true);
        }

        checkin = checkinRepository.save(checkin);

        log.info(SlackAppender.POST_TO_SLACK, "{} has checked {} at {}",
                person.getName() == null ? "A new user" : "User ' " + person.getName() + "'",
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

        return new ResponseEntity<>(isCheckedIn(person), HttpStatus.OK);
    }
}