package at.refugeescode.checkin;

import lombok.*;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@ToString
public class Person {

    @Id
    @GeneratedValue
    private Long id;

    @NonNull
    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

}
