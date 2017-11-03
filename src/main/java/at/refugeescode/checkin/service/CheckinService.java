package at.refugeescode.checkin.service;

import at.refugeescode.checkin.domain.Checkin;
import at.refugeescode.checkin.domain.CheckinRepository;
import at.refugeescode.checkin.domain.Person;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class CheckinService {

    @NonNull
    private final CheckinRepository checkinRepository;

    public Optional<Checkin> lastCheck(Person person) {
        List<Checkin> checkins = checkinRepository.findByPersonOrderByTime(person);
        return checkins.isEmpty() ? Optional.empty() : Optional.of(checkins.get(checkins.size() - 1));
    }

    public boolean isCheckedIn(Person person) {
        return lastCheck(person).map(Checkin::isCheckedIn).orElse(false);
    }

    public Duration getLastDuration(Person person) {
        return lastCheck(person).map(Checkin::getDuration).orElse(Duration.ZERO);
    }
}
