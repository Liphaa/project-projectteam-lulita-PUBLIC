package proj.concert.service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proj.concert.common.dto.ConcertDTO;
import proj.concert.common.dto.PerformerDTO;
import proj.concert.common.dto.SeatDTO;
import proj.concert.common.types.BookingStatus;
import proj.concert.common.types.Genre;
import proj.concert.service.domain.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;


@Path("/concert-service")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class ConcertResource {

    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);

    @GET
    @Path("/concerts")
    public Response getAllConcerts() {
        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {
            TypedQuery<Concert> query = em.createQuery("select c from Concert c", Concert.class);
            List<Concert> concerts = query.getResultList();
            List<ConcertDTO> concertDTOs = new ArrayList<>();
            for(Concert c : concerts) {
                concertDTOs.add(concertToDto(c));
            }
            return Response.ok(concertDTOs).build();

        } catch(Exception e) {
            return Response.serverError().build();
        }
        finally {
            em.close();
        }
    }

    @GET
    @Path("/seats/{date}") //3 in one function to get seats
    public Response getSeats(@QueryParam("status") BookingStatus stat, @PathParam("date") String dateStr) {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        TypedQuery<Seat> query;

        try {
            LocalDateTime date = LocalDateTime.parse(dateStr);
            if(stat == BookingStatus.Booked) {
             query = em.createQuery("select s from Seat s where s.date = :date and s.isBooked = true", Seat.class);
            }
            else if (stat == BookingStatus.Unbooked) {
                    query = em.createQuery("select s from Seat s where s.date = :date and s.isBooked = false", Seat.class);
                }
            else if (stat == BookingStatus.Any){
                    query = em.createQuery("select s from Seat s where s.date = :date", Seat.class);
                }
            else {return Response.status(Response.Status.BAD_REQUEST).entity("Invalid status").build(); }
            query.setParameter("date", date);
            List<Seat> seats = query.getResultList();

            if(seats.isEmpty()) {  return Response.status(Response.Status.NOT_FOUND).build(); }

            List<SeatDTO> seatDTOs = new ArrayList<>(); //convert seat objects into seat DTO
            for(Seat s : seats) {
                seatDTOs.add(seatToDto(s));
            }
            return Response.ok(seatDTOs).build();
        }
        catch(Exception e) {
            return Response.serverError().build();
        }
        finally {
            em.close();
        }
    }




    public static ConcertDTO concertToDto(Concert c){
        ConcertDTO dto = new ConcertDTO(c.getId(), c.getTitle(), c.getImageName(), c.getBlurb());
        List<LocalDateTime> mainList = new ArrayList<>(c.getDates());
        dto.setDates(mainList);
        List<PerformerDTO> performer_list = new ArrayList<>();

        for(Performer p : c.getPerformers()) {
            performer_list.add(performerToDto(p));
        }
        dto.setPerformers(performer_list);
        return dto;
    }
    public static PerformerDTO performerToDto(Performer p) {
        return new PerformerDTO(p.getId(), p.getName(), p.getImageName(), p.getGenre(), p.getBlurb());
    }

    public static SeatDTO seatToDto(Seat s) {
        return new SeatDTO(s.getLabel(), s.getPrice());
    }
}
