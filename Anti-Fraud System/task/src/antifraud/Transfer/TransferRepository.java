package antifraud.Transfer;

import antifraud.Card.StolenCard;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransferRepository extends CrudRepository<Transfer, Long> {

    Optional<Transfer> findTransferByCardNumber(String cardNumber);

    Optional<Transfer> findTransferByIpAddress(String ipAddress);

    Optional<Transfer> findTransferByRegion(String region);

    List<Transfer> findTransactionByCardNumberAndTransactionDateBetween(String cardNumber, String start, String end);

    List<Transfer> findAllByOrderByIdAsc();
}
