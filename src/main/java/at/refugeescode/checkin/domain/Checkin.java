package at.refugeescode.checkin.domain;

import lombok.*;

import javax.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
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
    @Column(nullable = false)
    private LocalDateTime time;

    @NonNull
    @Column(nullable = false)
    private Duration duration;

    private boolean checkedIn;

    private boolean auto;

    public Checkin(Person person, LocalDateTime time, Duration duration, boolean checkedIn, boolean auto) {
        this.person = person;
        this.time = time;
        this.duration = duration;
        this.checkedIn = checkedIn;
        this.auto = auto;
    }
}
