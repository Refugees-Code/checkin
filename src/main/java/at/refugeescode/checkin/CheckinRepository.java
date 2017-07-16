package at.refugeescode.checkin;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(collectionResourceRel = "checkins", path = "checkins")
public interface CheckinRepository extends CrudRepository<Checkin, Long> {

    List<Checkin> findByPersonOrderByTime(Person person);
}
