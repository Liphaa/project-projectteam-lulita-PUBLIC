package proj.concert.service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import proj.concert.common.dto.*;
import proj.concert.common.types.BookingStatus;
import proj.concert.common.types.Genre;
import proj.concert.service.domain.*;

import javax.persistence.LockModeType;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
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
                //return a 404 if concert not in database
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
                //return 404 if no performer found
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
            //return 401 if unauthorised booking attempted
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
                // if no token, return 403 error

                em.getTransaction().rollback();
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            User user = token.getUser();
            Concert concert = em.find(Concert.class, bookingRequest.getConcertId());
            // check if the concert exists to bok
            if (concert == null) {
                em.getTransaction().rollback();
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            LocalDateTime date = bookingRequest.getDate();
            //make sure date exists for specific booking
            if (!concert.getDates().contains(date)) {
                em.getTransaction().rollback();
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            List<Seat> reservedSeats = new ArrayList<>();
            //to book each seat
            for (String seatLabel : bookingRequest.getSeatLabels()) {
                TypedQuery<Seat> seatQuery = em.createQuery(
                                "SELECT s FROM Seat s WHERE s.label = :label AND s.date = :date", Seat.class)
                        .setParameter("label", seatLabel)
                        .setParameter("date", date)
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE); //When booking seats prevents other people from being able to look at it
                Seat seat;
                try {
                    seat = seatQuery.getSingleResult();
                } catch (NoResultException e) {
                    em.getTransaction().rollback();
                    return Response.status(Response.Status.FORBIDDEN).build();
                }

                if (seat.isBooked()) {
                    //transaction cannot go through if one of the seats have been booked
                    em.getTransaction().rollback();
                    return Response.status(Response.Status.FORBIDDEN).build();
                }


                seat.setBooked(true);
                reservedSeats.add(seat);
            }

            Booking booking = new Booking(user, concert, date, reservedSeats);
            //to associate the booked seats with the current booking
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

            processSubscription(concert, date);
            return Response.created(bookingUri)  // <-- this sets 201 + Location header
                    .entity(bookingDTO)
                    .build();

        } finally {
            em.close();
        }
    }

    @GET
    @Path("/bookings/{id}")
    public Response getBookingById(@PathParam("id") long id, @CookieParam("auth") Cookie clientId){
        EntityManager em = PersistenceManager.instance().createEntityManager();

        //return UNAUTHORIZED response when attempting to retrieve all bookings without logging in
        if (clientId == null){
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            em.getTransaction().begin();

            //Get the client from the token
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

            //Query for the booking with the given ID
            TypedQuery<Booking> bookingQuery = em.createQuery(
                    "SELECT b FROM Booking b " +
                            "JOIN FETCH b.reservedSeats " +
                            "JOIN FETCH b.user " +
                            "JOIN FETCH b.concert " +
                            "WHERE b.id = :id", Booking.class);
            bookingQuery.setParameter("id", id);
            Booking booking;
            try {
                booking = bookingQuery.getSingleResult();
            } catch (NoResultException e) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
            //if the user of the booking is different to the logged in user, return FORBIDDEN response
            if (booking.getUser().getId() != user.getId()) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            //Create bookingDTO object from the booking object to return in the response
            List<Seat> seats = booking.getReservedSeats();
            List<SeatDTO> seatDTOs = new ArrayList<>();
            for (Seat seat : seats) {
                seatDTOs.add(new SeatDTO(seat.getLabel(), seat.getPrice()));
            }
            //System.out.println("SEATDTOS:" + seatDTOs); - Testing for seatDTOS
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

        //return UNAUTHORIZED response when attempting to retrieve all bookings without logging in
        if (clientId == null){
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        try {
            em.getTransaction().begin();

            //Get the client from the token
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

            //Query for all bookings of the user found from the token, join fetch for some attributes of booking to solve n+1 problem
            TypedQuery<Booking> bookingQuery = em.createQuery(
                    "SELECT DISTINCT b FROM Booking b " +
                            "JOIN FETCH b.reservedSeats s " +
                            "JOIN FETCH b.concert " +
                            "WHERE b.user = :user", Booking.class);
            bookingQuery.setParameter("user", user);
            List<Booking> bookings = bookingQuery.getResultList();
            //if no booking is found, return an empty list
            if (bookings == null || bookings.isEmpty()) {
                return Response.ok(Collections.emptyList()).build();
            }

            //Creating a list of bookingDTO objects from the list of booking objects to return in the response
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
            //Query for the user
            TypedQuery<User> query = em.createQuery(
                            "SELECT u FROM User u WHERE u.username = :username AND u.password = :password", User.class)
                    .setParameter("username", creds.getUsername()).setParameter("password", creds.getPassword());
            User user;
            //if no user is found with the given password and/or username, UNAUTHORIZED status is returned
            try {
                user = query.getSingleResult();
            } catch (NoResultException e) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }

            //If the correct password and username are provided, a token is created for that user and attach to them via AuthToken class
            String token = UUID.randomUUID().toString();
            AuthToken authToken = new AuthToken(token, user);
            em.getTransaction().begin();
            em.persist(authToken);
            em.getTransaction().commit();

            //Create a cookie called "auth" and attach it to response
            NewCookie cookie = new NewCookie("auth", token);
            return Response.ok().cookie(cookie).build();

        } finally {
            em.close();
        }

    }

    @POST
    @Path("/subscribe/concertInfo")
    public void subscribeConcertInfo(@Suspended AsyncResponse sub, @CookieParam("auth") Cookie clientId, ConcertInfoSubscriptionDTO subscriptionDTO) {
        if (clientId == null) {
            sub.resume(Response.status(Response.Status.UNAUTHORIZED).entity("Missing auth cookie").build());
            return;
        }

        EntityManager em = PersistenceManager.instance().createEntityManager();
        try {
            em.getTransaction().begin();
            Concert concert;

            concert = em.find(Concert.class, subscriptionDTO.getConcertId());
            if (concert == null) {
                sub.resume(Response.status(Response.Status.BAD_REQUEST).build());
                return;
            }

            Set<LocalDateTime> dates = concert.getDates();
            if (!dates.contains(subscriptionDTO.getDate())) {
                sub.resume(Response.status(Response.Status.BAD_REQUEST).build());
                return;
            }

            TypedQuery<AuthToken> tokenQuery = em.createQuery(
                            "SELECT t FROM AuthToken t WHERE t.token = :token", AuthToken.class)
                    .setParameter("token", clientId.getValue());

            AuthToken token;
            try {
                token = tokenQuery.getSingleResult();
            } catch (NoResultException e) {
                sub.resume(Response.status(Response.Status.FORBIDDEN).build());
                return;
            }


            User user = token.getUser();
            Subscription subscription = new Subscription(user, concert, subscriptionDTO.getDate(), subscriptionDTO.getPercentageBooked());


            em.persist(subscription);
            subs.add(new ActiveSubscription(subscription, sub));
            em.getTransaction().commit();

        } finally {
            em.close();
        }
    }


    private void processSubscription(Concert concert, LocalDateTime date) {
        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {
            em.getTransaction().begin();

            //Querying for all seats
            List<Seat> seats = em.createQuery(
                            "SELECT s FROM Seat s WHERE s.date = :date", Seat.class)
                    .setParameter("date", date).getResultList();

            //Have all seats that are booked out in a list
            List<Seat> bookedSeats = new ArrayList<>();
            for (Seat s : seats) {
                if (s.isBooked()) {
                    bookedSeats.add(s);
                }
            }

            //calculate percentage of booked seats = number of bookedSeats divided by total number of seats
            int currentPercentage = (int) (((double) bookedSeats.size() / seats.size()) * 100);

            //Testing for calculations
            //System.out.println("Testing for calculation" +( seats.size() - bookedSeats.size()));
            //System.out.println("Current booking %: " + currentPercentage);

            //Testing the size of subs list
            //System.out.println("Size: " + subs.size());

            //Run a loop through each subscription in subs list and resume response once threshold of subscription is met
            for (ActiveSubscription sub : subs) {
                Subscription s = sub.getSubscription();
                if (s.getConcert().getId().equals(concert.getId()) && s.getDate().equals(date) &&
                        currentPercentage >= s.getPercentageBooked()) {
                    sub.getAsyncResponse().resume(new ConcertInfoNotificationDTO(seats.size() - bookedSeats.size()));
                }
            }

            em.getTransaction().commit();

        }finally {
            em.close();
        }

    }

}
