package dk.dbc.saturn.api;

public class PasswordEntryFrontEnd {
    private String host;
    private String username;
    private String password;
    private String activeFrom;

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

    public String getActiveFrom() {
        return activeFrom;
    }

    public void setActiveFrom(String activeFrom) {
        this.activeFrom = activeFrom;
    }

    public PasswordEntryFrontEnd withHost(String host) {
        this.host = host;
        return this;
    }

    public PasswordEntryFrontEnd withUsername(String username) {
        this.username = username;
        return this;
    }

    public PasswordEntryFrontEnd withPassword(String password) {
        this.password = password;
        return this;
    }

    public PasswordEntryFrontEnd withActiveFrom(String activeFrom) {
        this.activeFrom = activeFrom;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PasswordEntryFrontEnd that = (PasswordEntryFrontEnd) o;

        if (!host.equals(that.host)) return false;
        if (!username.equals(that.username)) return false;
        if (!password.equals(that.password)) return false;
        return activeFrom.equals(that.activeFrom);
    }

    @Override
    public int hashCode() {
        int result = host.hashCode();
        result = 31 * result + username.hashCode();
        result = 31 * result + password.hashCode();
        result = 31 * result + activeFrom.hashCode();
        return result;
    }
}
