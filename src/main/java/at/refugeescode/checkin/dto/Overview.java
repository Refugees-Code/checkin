package at.refugeescode.checkin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.YearMonth;
import java.util.List;

@AllArgsConstructor
@Getter
public class Overview {

    protected YearMonth yearMonth;
    protected List<String> columns;
    protected List<Attendance> attendance;
}
