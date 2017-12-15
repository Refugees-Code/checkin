package at.refugeescode.checkin.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class PersonOverview {

    protected String name;
    protected List<OverviewDuration> attendance;

}
