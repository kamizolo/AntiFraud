package antifraud.Transfer;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "transfer")
public class Transfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotNull
    int amount;

    @NotBlank
    String ipAddress;

    @NotBlank
    String cardNumber;

    @NotBlank
    String region;

    @NotBlank
    String transactionDate;

    @NotNull
    String result;

    @NotNull
    String feedback;

    public Transfer(long id, int amount, String cardNumber, String ipAddress, String region, String transactionDate, String result, String feedback) {
        this.id = id;
        this.amount = amount;
        this.cardNumber = cardNumber;
        this.ipAddress = ipAddress;
        this.region = region;
        this.transactionDate = transactionDate;
        this.result = result;
        this.feedback = feedback;
    }

    public Transfer() {
        feedback = "";
    }



    @JsonProperty("transactionId")
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
    @JsonProperty("number")
    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }
    @JsonProperty("ip")
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
    @JsonProperty("date")
    public String  getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String  transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}
