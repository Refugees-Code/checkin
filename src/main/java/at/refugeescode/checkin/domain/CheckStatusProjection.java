package at.refugeescode.checkin.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

@Projection(name = "status", types = Check.class)
public interface CheckStatusProjection {

    boolean isCheckedIn();

    @Value("#{target.person.name}")
    String getName();

}
