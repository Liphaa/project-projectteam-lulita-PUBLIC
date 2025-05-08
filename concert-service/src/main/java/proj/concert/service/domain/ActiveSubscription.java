package proj.concert.service.domain;

import javax.ws.rs.container.AsyncResponse;


public class ActiveSubscription {
    private Subscription subscription;
    private AsyncResponse asyncResponse;


    public ActiveSubscription(Subscription subscription, AsyncResponse asyncResponse) {
        this.subscription = subscription;
        this.asyncResponse = asyncResponse;
    }


    public Subscription getSubscription() {
        return subscription;
    }


    public AsyncResponse getAsyncResponse() {
        return asyncResponse;
    }
}

