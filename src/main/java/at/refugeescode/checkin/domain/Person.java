package at.refugeescode.checkin.domain;

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
@ToString(of = {"uid", "name", "email"})
public class Person {

    @Id
    @GeneratedValue
    private Long id;

    @NonNull
    @NotBlank
    @Column(nullable = false, unique = true)
    private String uid;

    @NonNull
    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @NonNull
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String slackHandle;

}
