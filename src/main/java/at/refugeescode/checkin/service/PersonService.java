package at.refugeescode.checkin.service;

import at.refugeescode.checkin.domain.Person;
import at.refugeescode.checkin.domain.PersonRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class PersonService {

    @NonNull
    private final PersonRepository personRepository;

    public List<Person> findNonNewUsers() {
        return personRepository.findByNameIsNotLike("new-user-%");
    }
}
