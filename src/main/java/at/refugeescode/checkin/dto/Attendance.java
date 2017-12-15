package at.refugeescode.checkin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class Attendance {

    protected String name;
    protected List<String> durations;

}
