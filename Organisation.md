## **Stage 1: Domain Construction**  
For domain construction all team members had a team meeting over Instagram on 16th April at 2:20pm to discuss plans on constructing the domain model. 
Luna was in charge of writing down group choices on:
- Relationships between each domain model
- Fields needed in domain classes
- Cascade requirements
- Json considerations
- Personal Deadlines implemented (Stage 2 finished by 17th April and Stage 3 due on 21st April)

After a mutual agreement on all 3 team members, Linh implemented the domain model during the call in Java given the pseudocode given by the others as well as the discussion on how the relationship should be implemented.

We designed our domain model with six entities: Concert, Seat, Performer, User, Booking, and AuthToken.
- Concert-Performer:  
  Concert has a many-to-many relationship with Performer. This relationship is bidirectional where each Concert references a set of Performer objects, and each Performer references a set of Concert objects.
- Concert-Date-Seat:  
  A Concert can occur on multiple dates, so it contains a collection of LocalDateTime - date objects. Since each date corresponds to one concert performance, the Seat entity does not have a direct link to Concert. Instead, it has a date attribute. This design links Seat and Concert indirectly through the concert date (each date has its own unique set of seats). As a result, Concert does not contain a set of seats.
- Booking-Seat:  
The Seat entity has a reference to a Booking, and Booking contains a set of seats,, hence, this is a bidirectional relationship. It allows navigation from a booking to its seats and vice versa.
- User-Booking:  
User has a one-to-many relationship with Booking where each user can have multiple bookings. This relationship is also bidirectional: each Booking references its owning User.
- AuthToken-User:  
AuthToken is a separate entity used to authenticate users. It contains a reference to a User, but the relationship is unidirectional which means only AuthToken references User, and not the other way around. This simplifies the design since users don’t need to track their tokens.

Cascade and Fetching:   
  Once domain models were constructed, we implemented lazy fetching for all because we do not want to add overheads to the system. We use join fetch when we want to access attributes of an object to increase efficiency (to resolve n+1 problem) 
For cascade, we use:  
- cascade.All for User and Booking because without an user a booking cannot exist. 
- cascade.Persist between booking and reserved seats because once a booking is deleted we don't want the seat to be deleted as well. 
- cascade.Persist for concert and performer because they have a many to many relationship.


## **Stage 2: Implementation of web services**  
For this task we’ve agreed that it would be easier to split up into individual tasks and implement them one at a time and merge at the end. We brainstormed the endpoints we would need based on the task given as well as the concertResourceIT class and allocated it among ourselves with an even distribution on difficulty levels.  
Luna: 
- Get all performers
- Login method
- Get booking from id
- Get all bookings.

Tanusha:
- Get single concert
- Get concert summaries
- Get single performer
- Make booking

Linh: 
- Get all concerts
- Getting booked/unbooked/all seats across all concerts.

To minimize concurrency errors, Linh implemented the use of setting lock modes to stop other users from accessing the current resource we’re on while trying to book. This is done by adding pessimistic locking to the booking method.  

Once we had all implemented our allocated methods, a testing branch was created to make sure that all of the methods interact directly with each other, ensured that there was overall consistency between all of our given methods, such as all using the same DTO mappers in concertResource and making small edits to domain models as seen appropriate


## **Stage 3: publish / subscribe feature**  
To support client subscriptions for concert booking updates, we created a wrapper class called ActiveSubscription, which pairs a Subscription object with an AsyncResponse. This allows each client's subscription request to be associated with a specific concert and date (from the Subscription), while the AsyncResponse lets us resume the client’s request later with a notification.  

In the ConcertResource class, we maintain a list of ActiveSubscription objects. Each time a client sends a subscription request to the subscribeConcertInfo endpoint, a new ActiveSubscription is added to this list. After each successful booking, we call the processSubscription method. This method checks whether the percentage of seats booked for a concert on a specific date has exceeded any client's threshold (the threshold is included in the Subscription object). If it has, the corresponding AsyncResponse is resumed with a notification to inform the client that their condition has been met.

The task was splitted evenly with us having an online meeting over Instagram call to discuss and brainstorm this feature: 
- Tanusha was in charge of creating the Subscription class and ActiveSubscription class domain models.
- Linh was in charge of creating the subscribeConcertInfo endpoint.
- Luna was in charge of creating the processSubscription method.

Finally, a touch-up branch was implemented to make final changes, such as adding comments for clarity of methods and removing unnecessary boundaries. 
  







