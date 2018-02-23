package at.refugeescode.checkin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Duration;

@AllArgsConstructor
@Getter
public class Summary {

    protected String name;
    protected boolean checkedIn;
    protected boolean auto;
    protected Duration lastDuration;
    protected Duration weekDuration;

}
