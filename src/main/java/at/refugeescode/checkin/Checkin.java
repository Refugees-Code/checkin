package at.refugeescode.checkin;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Checkin {

    @Id
    @GeneratedValue
    private Long id;

    @NonNull
    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Person person;

    @NonNull
    private LocalDateTime time;

    private boolean checkedIn;

    public Checkin(Person person, LocalDateTime time, boolean checkedIn) {
        this.person = person;
        this.time = time;
        this.checkedIn = checkedIn;
    }
}
