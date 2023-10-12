package antifraud.User;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;


@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(unique = true, name = "username")
    private String username;
    @NotBlank
    @Column(name = "name")
    private String name;
    @JsonIgnore
    @NotBlank
    @Column(name = "password")
    private String password;

    @NotBlank
    @Column(name = "authority")
    private String authority;

    @JsonIgnore
    private boolean access;



    public User() {
    }

    public User(long id, String name, String username, String password, String authority) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.password = password;
        this.authority = authority;
        this.access = false;

    }

    // getters and setters


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    @JsonProperty("role")
    public String getAuthority() {
        return authority.substring(5);
    }
    public String getRole() {
        return authority;
    }
    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public boolean getAccess() {
        return access;
    }
    public void setAccess(boolean access) { this.access = access; }
}