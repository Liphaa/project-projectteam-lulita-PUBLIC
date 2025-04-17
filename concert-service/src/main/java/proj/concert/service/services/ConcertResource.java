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



import java.util.UUID;

@Path("/concert-service")
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class ConcertResource {

    // TODO Implement this.
    private static Logger LOGGER = LoggerFactory.getLogger(ConcertResource.class);

    @Path("")
    public Response retrieveConcert(){
        throw new NotImplementedException("retrieveConcert");
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
    public Response retrievePerformer(){
        throw new NotImplementedException("retrievePerformer");
    }

    @Path("")
    public Response retrievePerformers(){
        throw new NotImplementedException("retrievePerformers");
    }

    @Path("")
    public Response book(){
        throw new NotImplementedException("book");
    }

    @Path("")
    public Response getBookingById(){
        throw new NotImplementedException("getBookingById");
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

            NewCookie cookie = new NewCookie("auth", UUID.randomUUID().toString());
            return Response.ok().cookie(cookie).build();

        } finally {
            em.close();
        }

    }


}
