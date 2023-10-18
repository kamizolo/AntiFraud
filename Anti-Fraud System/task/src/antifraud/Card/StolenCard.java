package antifraud.Card;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;

@Entity
@Table(name = "cards")
public class StolenCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @NotBlank
    String number;

    public StolenCard(long id, String number) {
        this.id = id;
        this.number = number;
    }

    public StolenCard() {

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }
}
