package proj.concert.service.domain;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Entity
public class Performer implements Comparable<Performer> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String imageName;
    private String blurb;

    @Enumerated(EnumType.STRING)
    private Genre genre;

    @ManyToMany(mappedBy = "performers", cascade = CascadeType.PERSIST)
    private Set<Concert> concerts = new HashSet<>();

    public Performer(Long id, String name, String imageName, String blurb, Genre genre) {
        this.id = id;
        this.name = name;
        this.imageName = imageName;
        this.blurb = blurb;
        this.genre = genre;

    }

    public Performer(String name, Genre genre) {
        this(null, name, null, null, genre);
    }

    public Performer() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageName() {return imageName;}

    public void setImageName(String imageName) {this.imageName = imageName;}

    public String getBlurb() {return blurb;}

    public void setBlurb(String blurb) { this.blurb = blurb;}

    public Set<Concert> getConcerts() {
        return concerts;
    }

    public void setConcerts(Set<Concert> concerts) {
        this.concerts = concerts;
    }


    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("Concert, id: ");
        buffer.append(id);
        buffer.append(", name: ");
        buffer.append(name);
        buffer.append(", about: ");
        buffer.append(blurb);
        buffer.append(", performing in: ");
        buffer.append(concerts.toString());
        return buffer.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Performer))
            return false;
        if (obj == this)
            return true;

        Performer rhs = (Performer) obj;
        return new EqualsBuilder().
                append(name, rhs.name).
                isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(name).hashCode();
    }

    @Override
    public int compareTo(Performer performer) {
        return name.compareTo(performer.getName());
    }

}