package antifraud.Transfer;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank
    String amount;

    @NotBlank
    String cardNumber;

    @NotBlank
    String ipAddress;

    @NotBlank
    String region;

    @NotBlank
    @Temporal(TemporalType.TIMESTAMP)
    LocalDateTime transactionDate;

    public Transfer(long id, String amount, String cardNumber, String ipAddress, String region, LocalDateTime transactionDate) {
        this.id = id;
        this.amount = amount;
        this.cardNumber = cardNumber;
        this.ipAddress = ipAddress;
        this.region = region;
        this.transactionDate = transactionDate;
    }

    public Transfer() {

    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public LocalDateTime  getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime  transactionDate) {
        this.transactionDate = transactionDate;
    }
}
