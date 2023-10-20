package antifraud.Card;

import org.hibernate.validator.constraints.UniqueElements;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "activecards")
public class ActiveCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @NotBlank
    String number;

    @NotNull
    int maxAllowed;

    @NotNull
    int maxManual;

    public ActiveCard(long id, String number, int maxAllowed, int maxManual) {
        this.id = id;
        this.number = number;
        this.maxAllowed = maxAllowed;
        this.maxManual = maxManual;
    }

    public ActiveCard() {
        maxAllowed = 200;
        maxManual = 1500;
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

    public int getMaxAllowed() {
        return maxAllowed;
    }

    public void setMaxAllowed(int maxAllowed) {
        this.maxAllowed = maxAllowed;
    }

    public int getMaxManual() {
        return maxManual;
    }

    public void setMaxManual(int maxManual) {
        this.maxManual = maxManual;
    }
}
