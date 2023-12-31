package ru.job4j.controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import ru.job4j.dto.*;
import ru.job4j.model.*;
import ru.job4j.service.*;

import java.lang.reflect.*;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.*;
import java.io.IOException;

@RestController
@RequestMapping("/person")
public class PersonController {

    private final SpringPersonService persons;
    private BCryptPasswordEncoder encoder;
    private static final Logger LOGGER = LogManager.getLogger(PersonController.class.getName());
    private final ObjectMapper objectMapper;

    public PersonController(final SpringPersonService persons,
                            ObjectMapper objectMapper,
                            BCryptPasswordEncoder encoder) {
        this.persons = persons;
        this.objectMapper = objectMapper;
        this.encoder = encoder;
    }

    @GetMapping("/")
    public List<Person> findAll() {
        return this.persons.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Person> findById(@PathVariable int id) {
        var person = this.persons.findById(id);
        return new ResponseEntity<Person>(
                person.orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account is not found. Please, check requisites."
                )),
                person.isPresent() ? HttpStatus.OK : HttpStatus.NOT_FOUND
        );
    }

//    Content-Type -> application/json
//
//            POST
//    {
//        "login": "newone",
//         "password": "password"
//    }
//    curl.exe -H "Content-Type: application/json" -X POST -d {"""login""":""""newone"""","""password""":""""password""""} "http://localhost:8080/person/"

    @PostMapping("/")
    @Validated(Operation.OnCreate.class)
    public ResponseEntity<Person> create(@Valid @RequestBody Person person) {
        var password = person.getPassword();
        person.setPassword(encoder.encode(password));
        return new ResponseEntity<Person>(
                this.persons.create(person),
                HttpStatus.CREATED
        );
    }

    @PutMapping("/")
    public ResponseEntity<Void> update(@Valid @RequestBody Person person) {
        var password = person.getPassword();
        person.setPassword(encoder.encode(password));
        if (this.persons.save(person)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        Person person = new Person();
        person.setId(id);
        if (this.persons.delete(person)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.badRequest().build();
    }

//    Content-Type -> application/json
//
//    POST
//    http://localhost:8080/login
//    {
//        "login": "admin",
//        "password": "password"
//    }
//   getting Bearer...

//    Authorization -> Bearer eyJ0eAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImV4cCIMTY5NDM5Nzk1NH0.fSMBqD2LlH3_LKSspmK6JddmeRyHKnYykzvPUBK43teT0U7Z9JtfnNdFapEEsT2sPKoI40SRpIrdM_S0C_CQIg

//    Content-Type -> application/json
//
//    PATCH
//    {
//        "id": 4,
//            "login": "admin",
//            "password": "password"
//    }

    @PatchMapping("/password")
    public Person newPassword(@Valid @RequestBody PersonDTO personDTO) throws InvocationTargetException, IllegalAccessException {
        String password = personDTO.getPassword();
//        if (password == null) {
//            throw new NullPointerException("Password mustn't be empty");
//        }
//        if (password.length() < 6) {
//            throw new IllegalArgumentException("Invalid password. Password length must be more than 5 characters.");
//        }
        var personOptional = persons.findById(personDTO.getId());
        if (personOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        var person = personOptional.get();
        person.setPassword(encoder.encode(password));
        persons.save(person);
        return person;
    }

    @ExceptionHandler(value = { IllegalArgumentException.class })
    public void exceptionHandler(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(new HashMap<>() { {
            put("message", e.getMessage());
            put("type", e.getClass());
        }}));
        LOGGER.error(e.getLocalizedMessage());
    }
}