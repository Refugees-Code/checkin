package at.refugeescode.checkin.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "checkins", path = "checkins")
public interface CheckRepository extends JpaRepository<Check, Long> {

    Page<Check> findByOrderByTimeDesc(Pageable pageable);

    Optional<Check> findFirstByPersonOrderByTimeDesc(Person person);

    Optional<Check> findFirstByPersonAndTimeBeforeOrderByTimeDesc(Person person, LocalDateTime end);

    Optional<Check> findFirstByCheckedInFalseAndPersonAndTimeBetweenOrderByTimeDesc(Person person, LocalDateTime start, LocalDateTime end);

    List<Check> findByPerson(Person person);

    List<Check> findByPersonOrderByTime(Person person);

    List<Check> findByPersonAndTimeBetweenOrderByTimeDesc(Person person, LocalDateTime start, LocalDateTime end);

    List<Check> findByPersonAndCheckedInFalseOrderByTime(Person person);

    List<Check> findByPersonAndCheckedInFalseAndTimeBetweenOrderByTimeDesc(Person person, LocalDateTime start, LocalDateTime end);

    List<Check> findByCheckedInFalseAndAutoFalseAndTimeBetweenOrderByTimeDesc(LocalDateTime start, LocalDateTime end);

}
