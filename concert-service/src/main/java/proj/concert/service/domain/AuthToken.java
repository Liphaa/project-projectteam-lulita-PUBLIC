package proj.concert.service.domain;

import javax.persistence.*;

@Entity
public class AuthToken {
    @Id
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    public AuthToken() {}

    public AuthToken(String token, User user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {return token;}
    public User getUser() {return user;}
    public void setUser(User user) {this.user = user;}
    public void setToken(String token) {this.token = token;}
}
