package antifraud.Card;

import antifraud.Ip.SuspiciousIp;
import antifraud.User.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface CardRepository extends CrudRepository<StolenCard, Long> {

    Optional<StolenCard> findCardByNumber(String number);
    void deleteCardByNumber(String number);

    List<StolenCard> findAllByOrderByIdAsc();
}
