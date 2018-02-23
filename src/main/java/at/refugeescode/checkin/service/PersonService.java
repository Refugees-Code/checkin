package at.refugeescode.checkin.service;

import at.refugeescode.checkin.domain.Check;
import at.refugeescode.checkin.domain.CheckRepository;
import at.refugeescode.checkin.domain.Person;
import at.refugeescode.checkin.domain.PersonRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class PersonService {

    @NonNull
    private final PersonRepository personRepository;
    @NonNull
    private final CheckRepository checkRepository;

    @Transactional(readOnly = true)
    public List<Person> findEnabledUsers() {
        return personRepository.findByDisabledFalse();
    }

    @Transactional(readOnly = false)
    public boolean setDisabled(String uid, boolean disabled) {
        Person person = personRepository.findByUid(uid);

        if (person == null)
            return false;

        person.setDisabled(disabled);
        personRepository.save(person);

        return true;
    }

    @Transactional(readOnly = false)
    public void delete(Person person) {
        List<Check> checks = checkRepository.findByPerson(person);
        checkRepository.delete(checks);
        personRepository.delete(person);
    }
}
