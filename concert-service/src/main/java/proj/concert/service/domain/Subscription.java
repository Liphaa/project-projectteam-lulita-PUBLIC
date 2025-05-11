package proj.concert.service.domain;


import javax.persistence.*;
import java.time.LocalDateTime;


@Entity
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @OneToOne(fetch = FetchType.LAZY)
    private Concert concert;


    private LocalDateTime date;


    private int percentageBooked;


    @ManyToOne
    private User user;




    public Subscription() {}


    public Subscription(User user, Concert concert, LocalDateTime date, int percentageBooked) {
        this.user = user;
        this.concert = concert;
        this.date = date;
        this.percentageBooked = percentageBooked;
    }


    public Long getId() {
        return id;
    }


    public Concert getConcert() {
        return concert;
    }
    public void setConcert(Concert concert) {
        this.concert = concert;
    }
    public LocalDateTime getDate() {
        return date;
    }
    public void setDate(LocalDateTime date) {
        this.date = date;
    }
    public int getPercentageBooked() {
        return percentageBooked;
    }
    public void setPercentageBooked(int percentageBooked) {
        this.percentageBooked = percentageBooked;
    }
    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }


}
