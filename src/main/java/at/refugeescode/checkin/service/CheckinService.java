package at.refugeescode.checkin.service;

import at.refugeescode.checkin.domain.Checkin;
import at.refugeescode.checkin.domain.CheckinRepository;
import at.refugeescode.checkin.domain.Person;
import at.refugeescode.checkin.domain.PersonRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class CheckinService {

    @NonNull
    private final CheckinRepository checkinRepository;
    @NonNull
    private final PersonRepository personRepository;

    public Optional<Checkin> lastCheck(Person person) {
        List<Checkin> checkins = checkinRepository.findByPersonOrderByTime(person);
        return checkins.isEmpty() ? Optional.empty() : Optional.of(checkins.get(checkins.size() - 1));
    }

    public boolean isCheckedIn(Person person) {
        return lastCheck(person).map(Checkin::isCheckedIn).orElse(false);
    }

    public Duration getLastCheckInTime(Person person) {
        LocalDateTime now = LocalDateTime.now();
        Optional<LocalDateTime> lastTime = lastCheck(person).map(Checkin::getTime);
        return lastTime.isPresent() ? Duration.between(lastTime.get(), now) : null;
    }

    @Transactional
    public Checkin newCheckin(String uid) {
        Person person = personRepository.findByUid(uid);

        if (person == null) {
            String placeholder = "new-user-" + LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
            person = personRepository.save(new Person(uid, placeholder, placeholder));
        }

        Optional<Checkin> lastCheckOptional = lastCheck(person);
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

        return checkin;
    }
}
