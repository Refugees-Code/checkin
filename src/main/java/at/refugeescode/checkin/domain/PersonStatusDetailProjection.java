package at.refugeescode.checkin.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.time.Duration;

@Projection(name = "detail-status", types = Person.class)
public interface PersonStatusDetailProjection {

    String getUid();

    String getName();

    @Value("#{@checkinService.isCheckedIn(target)}")
    boolean isCheckedIn();

    @Value("#{@checkinService.getLastCheckInTime(target)}")
    Duration getLastDuration();

    boolean isDisabled();

}
