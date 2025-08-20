package com.davidocmiller.eventsourcingdemo.store;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.davidocmiller.eventsourcingdemo.model.Event;

/**
 * EventStore
 */
public interface EventStore
{

    void store(Event event);

    Optional<Event> findById(UUID eventId);

    List<Event> downstreamEvents(Event root);

}
