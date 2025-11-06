package com.davidocmiller.eventsourcingdemo.store;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.davidocmiller.eventsourcingdemo.model.Event;

/**
 * InMemoryEventStore
 */
public class InMemoryEventStore implements EventStore
{

    private Map<UUID, Event> eventsById = new HashMap<>();
    private Map<UUID, List<Event>> downstreamEventsByParentId = new HashMap<>();
    private Set<Projection> toBeNotified = new HashSet<>();

    @Override
    public void store(Event event)
    {
        Objects.requireNonNull(event, "event cannot be null");
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

        for ( Projection p : toBeNotified )
        {
            p.notify(event);
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
        Objects.requireNonNull(root, "root cannot be null");
        return Collections.unmodifiableList(downstreamEventsByParentId.getOrDefault(
                root.getId(),
                Collections.emptyList()));
    }

    @Override
    public void register(Projection projection)
    {
        Objects.requireNonNull(projection, "Projection cannot be null");
        toBeNotified.add(projection);
    }

}
