package at.refugeescode.checkin.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "status", types = Person.class)
public interface PersonStatusProjection {

    String getName();

    @Value("#{@checkinService.isCheckedIn(target)}")
    boolean isCheckedIn();

}
