package proj.concert.service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.persistence.EntityManager;


import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.NotImplementedException;
import proj.concert.service.domain.*;
import proj.concert.common.dto.*;


import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Path("/concert-service")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class ConcertResource {

    // TODO Implement this.
    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);

    @GET
    @Path("/{id}")
    public Response getSingleConcert(@PathParam("id") long id) {
        EntityManager em = PersistenceManager.instance().createEntityManager();
        Response.ResponseBuilder responseBuilder;
        Concert c = em.find(Concert.class, id);
        if (c != null) {
            responseBuilder = Response.ok().entity(c);
        }
        else {
            responseBuilder = Response.status(Response.Status.NOT_FOUND);
        }
        em.close();
        return responseBuilder.build();
    }


    @Path("")
    public Response retrieveConcerts(){
        throw new NotImplementedException("retrieveConcerts");
    }

    @Path("")
    public Response retrieveConcertSummary(){
        throw new NotImplementedException("retrieveConcertSummaries");
    }

    @Path("")
    public Response retrievePerformer(@PathParam("id") long id){
        EntityManager em = PersistenceManager.instance().createEntityManager();
        Response.ResponseBuilder responseBuilder;
        Performer p = em.find(Performer.class, id);
        if (p != null) {
            responseBuilder = Response.ok().entity(p);
        }
        else {
            responseBuilder = Response.status(Response.Status.NOT_FOUND);
        }
        em.close();
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
        em.close();
        return performerDTOs;

    }

    @Path("")
    public Response book(){
        throw new NotImplementedException("book");
    }

    @Path("/bookings/{id}")
    public Response getBookingById(@PathParam("id") long id, @CookieParam("auth") Cookie clientId){
        EntityManager em = PersistenceManager.instance().createEntityManager();

        try {

            TypedQuery<AuthToken> tokenQuery = em.createQuery(
                    "SELECT t FROM AuthToken t WHERE t.token = :token", AuthToken.class)
                    .setParameter("token", clientId.getValue());
            AuthToken token;
            try{
                token = tokenQuery.getSingleResult();
            }catch(NoResultException e){
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            User user = token.getUser();

            Booking booking = em.find(Booking.class, id);
            if (booking == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }

            if (!booking.getUser().equals(user)) {
                return Response.status(Response.Status.FORBIDDEN).build();
            }

            List<Seat> seats = booking.getReservedSeats();
            List<SeatDTO> seatDTOs = new ArrayList<>();
            for (Seat seat : seats) {
                seatDTOs.add(new SeatDTO(seat.getLabel(), seat.getPrice()));
            }

            BookingDTO bookingDTO = new BookingDTO(booking.getConcert().getId(), booking.getDate(), seatDTOs );
            return Response.ok(bookingDTO).build();

        } finally {
            em.close();
        }
    }

    @Path("")
    public Response getAllBookings(){
        throw new NotImplementedException("getAllBookings");
    }

    @Path("")
    public Response getBookedSeats(){
        throw new NotImplementedException("getBookedSeats");
    }

    @Path("")
    public Response getUnbookedSeats(){
        throw new NotImplementedException("getUnbookedSeats");
    }

    @Path("")
    public Response getAllSeats(){
        throw new NotImplementedException("getAllSeats");
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
