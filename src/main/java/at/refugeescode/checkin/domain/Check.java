package at.refugeescode.checkin.domain;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@ToString
@Table(name = "checkin")
public class Check {

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

    private boolean checkedIn;

    private boolean auto;

    public Check(Person person, LocalDateTime time, boolean checkedIn, boolean auto) {
        this.person = person;
        this.time = time;
        this.checkedIn = checkedIn;
        this.auto = auto;
    }
}
