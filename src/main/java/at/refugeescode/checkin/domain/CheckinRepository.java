package at.refugeescode.checkin.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(collectionResourceRel = "checkins", path = "checkins")
public interface CheckinRepository extends JpaRepository<Checkin, Long> {

    List<Checkin> findByPersonOrderByTime(Person person);

    List<Checkin> findByPersonAndCheckedInTrueOrderByTime(Person person);
}
