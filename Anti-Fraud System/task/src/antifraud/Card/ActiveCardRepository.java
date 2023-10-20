package antifraud.Card;

import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface ActiveCardRepository extends CrudRepository<ActiveCard, Long> {

    Optional<ActiveCard> findActiveCardsByNumber(String number);
    void deleteCardByNumber(String number);

    List<ActiveCard> findAllByOrderByIdAsc();
}
