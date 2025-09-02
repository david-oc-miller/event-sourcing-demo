package com.davidocmiller.eventsourcingdemo.user;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.davidocmiller.eventsourcingdemo.model.Event;
import com.davidocmiller.eventsourcingdemo.store.Projection;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue.ValueType;

public class UserProjection implements Projection
{
    private Map<String, User> usersById = new HashMap<>();

    @Override
    public void notify(Event event)
    {
        Objects.requireNonNull(event, "event must not be null");
        if ("userRegistered".equals(event.getEventType()))
        {
            handleUserRegistered(event);
        }
    }

    public Optional<User> findById(String userId)
    {
        return Optional.ofNullable(usersById.get(userId));
    }
        

    private void handleUserRegistered(Event event)
    {
        if ( !event.getBody().getValueType().equals(ValueType.OBJECT) )
        {
            throw new IllegalArgumentException("Event body must be an object");
        }
        JsonObject userRegisteredEvent = event.getBody().asJsonObject();
        // TODO: verify against a schema
        String userId = userRegisteredEvent.getString("userid");
        String firstName = userRegisteredEvent.getString("firstName");
        if ( userId == null || userId.trim().isEmpty() || firstName == null || firstName.trim().isEmpty())
        {
            throw new IllegalArgumentException("userRegisteredEvent must have a user id and a first name");
        }
        User registered = new User(userId, firstName);
        usersById.put(userId, registered);
    }

}
