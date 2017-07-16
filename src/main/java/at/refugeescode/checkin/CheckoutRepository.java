package at.refugeescode.checkin;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.List;

@RepositoryRestResource(collectionResourceRel = "checkout", path = "checkout")
public interface CheckoutRepository extends CrudRepository<Checkout, Long> {

    List<Checkout> findByPersonOrderByTime(Person person);
}
