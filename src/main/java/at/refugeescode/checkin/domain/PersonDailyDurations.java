package at.refugeescode.checkin.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class PersonDailyDurations {

    protected String name;
    protected List<DailyDuration> dailyDurations;

}
