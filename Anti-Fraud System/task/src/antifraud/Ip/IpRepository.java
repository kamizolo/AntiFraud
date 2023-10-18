package antifraud.Ip;

import antifraud.User.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface IpRepository extends CrudRepository<SuspiciousIp, Long> {
    Optional<SuspiciousIp> findIpByIp(String ip);
    void deleteIpByIp(String username);

    List<SuspiciousIp> findAllByOrderByIdAsc();
}
