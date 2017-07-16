package at.refugeescode.checkin;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class AttendanceController {

    private PersonRepository personRepository;

    private CheckinRepository checkinRepository;

    private CheckoutRepository checkoutRepository;

    @Autowired
    public AttendanceController(
            PersonRepository personRepository,
            CheckinRepository checkinRepository,
            CheckoutRepository checkoutRepository) {
        this.personRepository = personRepository;
        this.checkinRepository = checkinRepository;
        this.checkoutRepository = checkoutRepository;
    }

    private Checkin checkin(Person person) {
        return checkinRepository.save(new Checkin(person, LocalDateTime.now()));
    }

    private Checkout checkout(Person person) {
        return checkoutRepository.save(new Checkout(person, LocalDateTime.now()));
    }

    @PostMapping("/people/{id}/checkin")
    public ResponseEntity<Checkin> checkin(@PathVariable("id") Long id) {

        Person person = personRepository.findOne(id);

        if (person == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        Checkin checkin = checkin(person);

        return new ResponseEntity<>(checkin, HttpStatus.OK);
    }

    @PostMapping("/people/{id}/checkout")
    public ResponseEntity<Checkout> checkout(@PathVariable("id") Long id) {

        Person person = personRepository.findOne(id);

        if (person == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        Checkout checkout = checkout(person);

        return new ResponseEntity<>(checkout, HttpStatus.OK);
    }

    @GetMapping("/people/{id}/status")
    public ResponseEntity<Map<String, Object>> status(@PathVariable("id") Long id) {

        Person person = personRepository.findOne(id);

        if (person == null)
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);

        List<Checkin> checkins = checkinRepository.findByPersonOrderByTime(person);
        List<Checkout> checkouts = checkoutRepository.findByPersonOrderByTime(person);

        if (checkins.isEmpty())
            return new ResponseEntity<>(
                    ImmutableMap.<String, Object>builder()
                            .put("lastCheckin", "[never]")
                            .put("checkedIn", false)
                            .build(),
                    HttpStatus.OK);

        Checkin lastCheckin = checkins.get(checkins.size() - 1);

        if (checkouts.isEmpty())
            return new ResponseEntity<>(
                    ImmutableMap.<String, Object>builder()
                            .put("lastCheckin", lastCheckin.getTime())
                            .put("lastCheckout", "[never]")
                            .put("checkedIn", true)
                            .build(),
                    HttpStatus.OK);

        Checkout lastCheckout = checkouts.get(checkouts.size() - 1);
        Boolean checkoutAfterCheckin = lastCheckout.getTime().isAfter(lastCheckin.getTime());

        return new ResponseEntity<>(
                ImmutableMap.<String, Object>builder()
                        .put("lastCheckin", lastCheckin.getTime())
                        .put("lastCheckout", lastCheckout.getTime())
                        .put("checkedIn", !checkoutAfterCheckin)
                        .build(),
                HttpStatus.OK);
    }
}
