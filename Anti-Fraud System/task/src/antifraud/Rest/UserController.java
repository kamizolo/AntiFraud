package antifraud.Rest;

import antifraud.User.CRUD.UserRepository;
import antifraud.User.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.util.Optional;

//controller for all user related requests
@RestController
public class UserController {
    @Autowired
    UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserController(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }
    //create user
    @PostMapping(path = "/api/auth/user")
    public ResponseEntity<String> register(@RequestBody RegistrationRequest request) throws JsonProcessingException {

        //check if it already exists
        if (repository.findUserByUsername(request.username()).isPresent()) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        //Field Missing
        if (request.username() == null || request.username().isBlank())
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        else if (request.name() == null || request.name().isBlank())
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        else if (request.password() == null || request.password().isBlank())
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        //create and save user
        var user = new User();
        user.setUsername(request.username());
        user.setName(request.name());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setAuthority(String.valueOf(new SimpleGrantedAuthority("ROLE_MERCHANT")));

        repository.save(user);

        ObjectMapper objectMapper = new ObjectMapper();
        Optional<User> u = repository.findUserByUsername(request.username());
        //sett first created user as admin
        if(u.get().getId() == 1){
            u.get().setAuthority("ROLE_ADMINISTRATOR");
            u.get().setAccess(true);
            repository.save(u.get());
        }
        return new ResponseEntity<>(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(u.get()), HttpStatus.CREATED);


    }
    //returns list of users
    @GetMapping(path = "/api/auth/list")
    public ResponseEntity<String>  list() throws JsonProcessingException {

        Iterable<User> users = repository.findAllByOrderByIdAsc(); //CRUD

        ObjectMapper objectMapper = new ObjectMapper();
        StringBuilder output = new StringBuilder();
        output.append("[\n");
        for (User u : users) {
            output.append(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(u)).append(",");
        }
        output.deleteCharAt(output.length() - 1);
        output.append("\n]");
        System.out.println(output);
        return new ResponseEntity<>(output.toString(), HttpStatus.OK);
    }
    //deletes user
    @Transactional
    @DeleteMapping(path = "/api/auth/user/{username}")
    public ResponseEntity<String> delete(@PathVariable String username) {

        Optional<User> deleting = repository.findUserByUsername(username);
        if (deleting.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        repository.deleteUserByUsername(username);

        String output = """
                {
                   "username": "%s",
                   "status": "Deleted successfully!"
                }""";
        return new ResponseEntity<>(output.formatted(username),HttpStatus.OK);
    }

    //update access
    @Transactional
    @PutMapping(path = "/api/auth/access")
    public ResponseEntity<String> setAccess(@RequestBody SetAccessRequest request) {
        Optional<User> user = repository.findUserByUsername(request.username());
        if (user.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        //administrator cant remove own access
        else if (user.get().getAuthority().equals("ADMINISTRATOR")) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        //checks if operation is legal
        else if (!request.operation.equals("LOCK") && !request.operation.equals("UNLOCK")) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        //update and save
        user.get().setAccess(request.operation.equals("UNLOCK"));
        repository.save(user.get());
        String output = """
                {
                    "status": "User %s %s!"
                }
                """;
        return new ResponseEntity<>(output.formatted(request.username, user.get().getAccess() ? "unlocked":"locked"),HttpStatus.OK);
    }
    //update role
    @Transactional
    @PutMapping(path = "/api/auth/role")
    public ResponseEntity<String> setRole(@RequestBody SetRoleRequest request) throws JsonProcessingException {
        Optional<User> user = repository.findUserByUsername(request.username());
        if (user.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        //administrator cant change role
        else if (user.get().getAuthority().equals("ADMINISTRATOR")) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        //checks if new role is valid
        else if (!request.role.equals("SUPPORT") && !request.role.equals("MERCHANT")) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        //checks if user already has that role
        else if(user.get().getAuthority().equals(request.role)) {
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }
        //update and save
        user.get().setAuthority("ROLE_" + request.role);
        repository.save(user.get());
        ObjectMapper objectMapper = new ObjectMapper();
        return new ResponseEntity<>(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(user.get()), HttpStatus.OK);
    }

    record RegistrationRequest(String username, String name, String password, String authority) { }
    record SetAccessRequest(String username, String operation) { }
    record SetRoleRequest(String username, String role) { }
}