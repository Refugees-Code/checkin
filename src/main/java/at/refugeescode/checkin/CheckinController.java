package at.refugeescode.checkin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@Slf4j
public class CheckinController {

    private PersonRepository personRepository;

    private CheckinRepository checkinRepository;

    @Autowired
    public CheckinController(
            PersonRepository personRepository,
            CheckinRepository checkinRepository) {
        this.personRepository = personRepository;
        this.checkinRepository = checkinRepository;
    }

    private boolean isCheckedIn(Person person) {
        List<Checkin> checkins = checkinRepository.findByPersonOrderByTime(person);
        return !checkins.isEmpty() && checkins.get(checkins.size() - 1).isCheckedIn();
    }

    @GetMapping("/people/{uid}/checkin")
    @Transactional
    public ResponseEntity<Checkin> checkin(@PathVariable("uid") String uid) {

        Person person = personRepository.findByUid(uid);

        if (person == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        Checkin checkin = checkinRepository.save(new Checkin(person, LocalDateTime.now(), !isCheckedIn(person)));

        return new ResponseEntity<>(checkin, HttpStatus.OK);
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
