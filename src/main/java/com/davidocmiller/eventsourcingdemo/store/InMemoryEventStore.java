package com.davidocmiller.eventsourcingdemo.store;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.davidocmiller.eventsourcingdemo.model.Event;

/**
 * InMemoryEventStore
 */
public class InMemoryEventStore implements EventStore
{

    private Map<UUID, Event> eventsById = new HashMap<>();
    
    @Override
    public void store(Event event)
    {
        if (eventsById.containsKey(event.getId()))
        {
            throw new IllegalArgumentException(String.format("This event is already stored: %s", event));
        }
        eventsById.put(event.getId(), event);
    }

    @Override
    public Optional<Event> findById(UUID eventId)
    {
        return Optional.of(eventsById.get(eventId));
    }

    
}
