package at.refugeescode.checkin;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@ToString
public class Checkout {

    @Id
    @GeneratedValue
    private Long id;

    @NonNull
    @ManyToOne(optional = false)
    @JoinColumn(nullable = false)
    private Person person;

    @NonNull
    private LocalDateTime time;

}
