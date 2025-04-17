package proj.concert.service.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.NotImplementedException;

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


}
