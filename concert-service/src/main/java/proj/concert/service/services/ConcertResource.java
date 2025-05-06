package proj.concert.service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proj.concert.common.dto.*;
import proj.concert.common.types.BookingStatus;
import proj.concert.common.types.Genre;
import proj.concert.service.domain.*;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.NoResultException;

import org.apache.commons.lang3.NotImplementedException;

import java.time.LocalDateTime;
import java.net.URI;
import java.util.*;
import java.util.Set;
import java.util.ArrayList;

@Path("/concert-service")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class ConcertResource {


    // TODO Implement this.
    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);
    private static final List<ActiveSubscription> subs = new Vector<>();


    @GET
    @Path("/concerts/{id}")
    public Response getSingleConcert(@PathParam("id") long id) {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        Response.ResponseBuilder responseBuilder;
        try {
            Concert c = em.find(Concert.class, id);
            if (c == null) {
                responseBuilder = Response.status(Response.Status.NOT_FOUND);
            } else {
                ConcertDTO concertDTO = concertToDto(c);
                responseBuilder = Response.ok().entity(concertDTO);
            }
        }
        finally {
            em.close();
        }
        return responseBuilder.build();
    }


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
    @Path("/concerts/summaries")
    public Response retrieveConcertSummary(){
        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            TypedQuery<Concert> query = em.createQuery("select c from Concert c", Concert.class);
            List<Concert> concerts = query.getResultList();
            List<ConcertSummaryDTO> concertSummaryDTOS = new ArrayList<>();
            for (Concert c : concerts) {
                // Create a new DTO for each concert and add it to the list
                ConcertSummaryDTO summaryDTO = new ConcertSummaryDTO(c.getId(), c.getTitle(), c.getImageName());
                concertSummaryDTOS.add(summaryDTO);
            }

            return Response.ok(concertSummaryDTOS).build();
        }
        catch (Exception e) {
            return Response.serverError().build();
        }
        finally {
            em.close();
        }

    }

    @GET
    @Path("/performers/{id}")
    public Response retrievePerformer(@PathParam("id") long id){
        EntityManager em = PersistenceManager.instance().createEntityManager();
        Response.ResponseBuilder responseBuilder;
        try {
            Performer p = em.find(Performer.class, id);
            if (p != null) {
                PerformerDTO performerDTO = performerToDto(p);
                responseBuilder = Response.ok().entity(performerDTO);
            } else {
                responseBuilder = Response.status(Response.Status.NOT_FOUND);
            }
        }
        finally{
                em.close();
            }
        return responseBuilder.build();

    }

    @GET
    @Path("/performers")
    public List<PerformerDTO> retrievePerformers(){
        EntityManager em = PersistenceManager.instance().createEntityManager();
        List<Performer> performers = em.createQuery("SELECT p FROM Performer p", Performer.class).getResultList();

        List<PerformerDTO> performerDTOs = new ArrayList<>();
        for (Performer performer : performers) {
            performerDTOs.add(new PerformerDTO(performer.getId(), performer.getName(), performer.getImageName(), performer.getGenre(), performer.getBlurb()));
        }
        System.out.println(performerDTOs);
        em.close();
        return performerDTOs;

    }

    @POST
    @Path("/bookings")
    public Response book(BookingRequestDTO bookingRequest, @CookieParam("auth") Cookie clientId) {
        if (clientId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Missing auth cookie").build();
        }

        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            em.getTransaction().begin();

            TypedQuery<AuthToken> tokenQuery = em.createQuery(
                            "SELECT t FROM AuthToken t WHERE t.token = :token", AuthToken.class)
                    .setParameter("token", clientId.getValue());

            AuthToken token;
            try {
                token = tokenQuery.getSingleResult();
            } catch (NoResultException e) {
                em.getTransaction().rollback();
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            User user = token.getUser();
            Concert concert = em.find(Concert.class, bookingRequest.getConcertId());

            if (concert == null) {
                em.getTransaction().rollback();
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            LocalDateTime date = bookingRequest.getDate();
            if (!concert.getDates().contains(date)) {
                em.getTransaction().rollback();
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            List<Seat> reservedSeats = new ArrayList<>();
            for (String seatLabel : bookingRequest.getSeatLabels()) {
                TypedQuery<Seat> seatQuery = em.createQuery(
                                "SELECT s FROM Seat s WHERE s.label = :label AND s.date = :date", Seat.class)
                        .setParameter("label", seatLabel)
                        .setParameter("date", date);
                Seat seat;
                try {
                    seat = seatQuery.getSingleResult();
                } catch (NoResultException e) {
                    em.getTransaction().rollback();
                    return Response.status(Response.Status.FORBIDDEN).build();
                }

                if (seat.isBooked()) {
                    em.getTransaction().rollback();
                    return Response.status(Response.Status.FORBIDDEN).build();
                }

                seat.setBooked(true);
                reservedSeats.add(seat);
            }

            Booking booking = new Booking(user, concert, date, reservedSeats);

            for (Seat seat : reservedSeats) {
                seat.setBooking(booking);
            }
            em.persist(booking);

            List<SeatDTO> seatDTOs = new ArrayList<>();
            for (Seat seat : booking.getReservedSeats()) {
                seatDTOs.add(new SeatDTO(seat.getLabel(), seat.getPrice()));
            }


            BookingDTO bookingDTO = new BookingDTO(concert.getId(), date, seatDTOs);


            em.getTransaction().commit();
            URI bookingUri = UriBuilder.fromUri("concert-service/bookings/{id}")
                    .build(booking.getId());


            return Response.created(bookingUri)  // <-- this sets 201 + Location header
                    .entity(bookingDTO)
                    .build();

        }catch (Exception e) {
            e.printStackTrace(); // log or send to logger
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            return Response.serverError().build();
        } finally {
            em.close();
        }
    }

    @GET
    @Path("/bookings/{id}")
    public Response getBookingById(@PathParam("id") long id, @CookieParam("auth") Cookie clientId){

        EntityManager em = PersistenceManager.instance().createEntityManager();
        if (clientId == null){
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            em.getTransaction().begin();

            TypedQuery<AuthToken> tokenQuery = em.createQuery(
                            "SELECT t FROM AuthToken t WHERE t.token = :token", AuthToken.class)
                    .setParameter("token", clientId.getValue());
            AuthToken token;
            try {
                token = tokenQuery.getSingleResult();
            } catch (NoResultException e) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            User user = token.getUser();

            Booking booking = em.find(Booking.class, id);
            if (booking == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            if (booking.getUser().getId() != user.getId()) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            List<Seat> seats = booking.getReservedSeats();
            List<SeatDTO> seatDTOs = new ArrayList<>();
            for (Seat seat : seats) {
                seatDTOs.add(new SeatDTO(seat.getLabel(), seat.getPrice()));
            }
            //System.out.println("BOOKEDDDD:" + seatDTOs);
            BookingDTO bookingDTO = new BookingDTO(booking.getConcert().getId(), booking.getDate(), seatDTOs);

            em.getTransaction().commit();
            return Response.ok().entity(bookingDTO).build();

        } finally {
            em.close();
        }
    }

    @GET
    @Path("/bookings")
    public Response getAllBookings(@CookieParam("auth") Cookie clientId) {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        if (clientId == null){
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            em.getTransaction().begin();
            TypedQuery<AuthToken> tokenQuery = em.createQuery(
                            "SELECT t FROM AuthToken t WHERE t.token = :token", AuthToken.class)
                    .setParameter("token", clientId.getValue());
            AuthToken token;
            try {
                token = tokenQuery.getSingleResult();
            } catch (NoResultException e) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            User user = token.getUser();
            Set<Booking> bookings = user.getBookings();
            if (bookings == null || bookings.isEmpty()) {
                return Response.ok(Collections.emptyList()).build();
            }
            List<BookingDTO> bookingDTOs = new ArrayList<>();
            List<Seat> seats;
            List<SeatDTO> seatDTOs;
            for (Booking booking : bookings) {
                seats = booking.getReservedSeats();
                seatDTOs = new ArrayList<>();
                for (Seat seat : seats) {
                    seatDTOs.add(new SeatDTO(seat.getLabel(), seat.getPrice()));
                }
                BookingDTO bookingDTO = new BookingDTO(booking.getConcert().getId(), booking.getDate(), seatDTOs);
                bookingDTOs.add(bookingDTO);
            }
            em.getTransaction().commit();

            return Response.ok(bookingDTOs).build();

        } finally {
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

    @GET
    @Path("/seats/{date}") //3 in one function to get seats
    public Response getSeats(@QueryParam("status") BookingStatus stat, @PathParam("date") String dateStr) {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        TypedQuery<Seat> query;

        try {
            LocalDateTime date = LocalDateTime.parse(dateStr);
            if (stat == BookingStatus.Booked) {
                query = em.createQuery("select s from Seat s where s.date = :date and s.isBooked = true", Seat.class);
            } else if (stat == BookingStatus.Unbooked) {
                query = em.createQuery("select s from Seat s where s.date = :date and s.isBooked = false", Seat.class);
            } else if (stat == BookingStatus.Any) {
                query = em.createQuery("select s from Seat s where s.date = :date", Seat.class);
            } else {
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid status").build();
            }
            query.setParameter("date", date);
            List<Seat> seats = query.getResultList();


            List<SeatDTO> seatDTOs = new ArrayList<>(); //convert seat objects into seat DTO
            for (Seat s : seats) {
                seatDTOs.add(seatToDto(s));
            }
            return Response.ok(seatDTOs).build();
        } catch (Exception e) {
            return Response.serverError().build();
        } finally {
            em.close();
        }
    }

    @POST
    @Path("/login")
    public Response login(UserDTO creds) {

        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {
            TypedQuery<User> query = em.createQuery(
                            "SELECT u FROM User u WHERE u.username = :username AND u.password = :password", User.class)
                    .setParameter("username", creds.getUsername()).setParameter("password", creds.getPassword());

            User user;
            try {
                user = query.getSingleResult();
            } catch (NoResultException e) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            String token = UUID.randomUUID().toString();
            AuthToken authToken = new AuthToken(token, user);
            em.getTransaction().begin();
            em.persist(authToken);
            em.getTransaction().commit();

            NewCookie cookie = new NewCookie("auth", token);
            return Response.ok().cookie(cookie).build();

        } finally {
            em.close();
        }

    }


}
