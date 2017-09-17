package at.refugeescode.checkin.domain;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;
import java.util.Optional;

@RepositoryRestResource(collectionResourceRel = "checkins", path = "checkins")
public interface CheckinRepository extends CrudRepository<Checkin, Long> {

    Optional<Checkin> findLastByPersonOrderByTime(Person person);

    List<Checkin> findByPersonOrderByTime(Person person);
}
