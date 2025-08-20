package com.davidocmiller.eventsourcingdemo.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
    private Map<UUID, List<Event>> downstreamEventsByParentId = new HashMap<>();

    @Override
    public void store(Event event)
    {
        if (eventsById.containsKey(event.getId()))
        {
            throw new IllegalArgumentException(String.format("This event is already stored: %s", event));
        }
        eventsById.put(event.getId(), event);

        for (Event source : event.getSourceEvents())
        {
            List<Event> downstream = downstreamEventsByParentId.getOrDefault(source.getId(), new ArrayList<>());
            downstream.add(event);
            downstreamEventsByParentId.put(source.getId(), downstream);
        }
    }

    @Override
    public Optional<Event> findById(UUID eventId)
    {
        return Optional.of(eventsById.get(eventId));
    }

    @Override
    public List<Event> downstreamEvents(Event root)
    {
        return Collections.unmodifiableList(downstreamEventsByParentId.getOrDefault(
                root.getId(),
                Collections.emptyList()));
    }

}
