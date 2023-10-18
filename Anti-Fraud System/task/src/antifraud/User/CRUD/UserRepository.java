package antifraud.User.CRUD;

import antifraud.User.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {
    Optional<User> findUserByUsername(String username);
    void deleteUserByUsername(String username);

    List<User> findAll();
    List<User> findAllByOrderByIdAsc();


}