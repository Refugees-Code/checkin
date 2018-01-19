package at.refugeescode.checkin.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "checkins", path = "checkins")
public interface CheckinRepository extends JpaRepository<Checkin, Long> {

    Page<Checkin> findByOrderByTimeDesc(Pageable pageable);

    Optional<Checkin> findFirstByPersonOrderByTimeDesc(Person person);

    Optional<Checkin> findFirstByPersonAndTimeBeforeOrderByTimeDesc(Person person, LocalDateTime end);

    List<Checkin> findByPersonOrderByTime(Person person);

    List<Checkin> findByPersonAndTimeBetweenOrderByTimeDesc(Person person, LocalDateTime start, LocalDateTime end);

    List<Checkin> findByPersonAndCheckedInFalseOrderByTime(Person person);

    List<Checkin> findByPersonAndCheckedInFalseAndTimeBetweenOrderByTimeDesc(Person person, LocalDateTime start, LocalDateTime end);

}
