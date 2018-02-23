package at.refugeescode.checkin.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import java.time.Duration;
import java.time.LocalDateTime;

@Projection(name = "log", types = Check.class)
public interface CheckProjection {

    Long getId();

    boolean isCheckedIn();

    boolean isAuto();

    LocalDateTime getTime();

    @Value("#{@checkService.getDuration(target)}")
    Duration getDuration();

    @Value("#{target.person.name}")
    String getName();

}
