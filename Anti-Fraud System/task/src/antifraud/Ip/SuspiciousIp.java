package antifraud.Ip;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
@Entity
@Table(name = "ips")
public class SuspiciousIp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @NotBlank
    String ip;

    public SuspiciousIp(long id, String ip) {
        this.id = id;
        this.ip = ip;
    }

    public SuspiciousIp() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
