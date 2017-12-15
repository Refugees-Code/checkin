package at.refugeescode.checkin.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;

@AllArgsConstructor
@Getter
public class WeeklyDuration implements OverviewDuration {

    protected int week;
    protected Duration duration;
}
