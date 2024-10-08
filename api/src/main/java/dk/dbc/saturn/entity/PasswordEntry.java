package dk.dbc.saturn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "passwords")
@NamedQueries({
        @NamedQuery(
                name = PasswordEntry.GET_PASSWORDS_NAME,
                query = PasswordEntry.GET_PASSWORDS_QUERY
        ),
        @NamedQuery(
                name = PasswordEntry.GET_PASSWORD_FOR_DATE_NAME,
                query = PasswordEntry.GET_PASSWORD_FOR_DATE_QUERY
        ),
        @NamedQuery(
                name = PasswordEntry.DELETE_PASSWORD_ENTRY_NAME,
                query = PasswordEntry.DELETE_PASSWORD_ENTRY_QUERY
        )


})
public class PasswordEntry {
    public static final String GET_PASSWORDS_NAME = "getPasswords";
    public static final String GET_PASSWORDS_QUERY =
        "SELECT e FROM PasswordEntry e WHERE e.host = :host and e.username = :username ORDER BY e.id";
    public static final String GET_PASSWORD_FOR_DATE_NAME = "getPasswordForDate";
    public static final String GET_PASSWORD_FOR_DATE_QUERY =
            "SELECT e FROM PasswordEntry e WHERE e.host = :host and e.username = :username and e.activeFrom < :date order by e.activeFrom desc";
    public static final String DELETE_PASSWORD_ENTRY_NAME = "deletePasswordEntry";
    public static final String DELETE_PASSWORD_ENTRY_QUERY = "DELETE FROM PasswordEntry e WHERE e.id = :id";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String host;
    private String username;
    private String password;

    @Column(name = "activefrom")
    private OffsetDateTime activeFrom;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public OffsetDateTime getActiveFrom() {
        return activeFrom;
    }

    public void setActiveFrom(OffsetDateTime activeFrom) {
        this.activeFrom = activeFrom;
    }

    public PasswordEntry withHost(String host) {
        this.host = host;
        return this;
    }

    public PasswordEntry withUsername(String username) {
        this.username = username;
        return this;
    }

    public PasswordEntry withPassword(String password) {
        this.password = password;
        return this;
    }

    public PasswordEntry withActiveFrom(OffsetDateTime activeFrom) {
        this.activeFrom = activeFrom;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PasswordEntry entry = (PasswordEntry) o;

        if (id != entry.id) return false;
        if (!host.equals(entry.host)) return false;
        if (!username.equals(entry.username)) return false;
        if (!password.equals(entry.password)) return false;
        return activeFrom.equals(entry.activeFrom);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + host.hashCode();
        result = 31 * result + username.hashCode();
        result = 31 * result + password.hashCode();
        result = 31 * result + activeFrom.hashCode();
        return result;
    }
}
