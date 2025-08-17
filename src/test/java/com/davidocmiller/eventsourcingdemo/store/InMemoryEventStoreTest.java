package com.davidocmiller.eventsourcingdemo.store;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.davidocmiller.eventsourcingdemo.model.Event;

import jakarta.json.JsonValue;

/**
 * InMemoryEventStoreTest
 */
public class InMemoryEventStoreTest
{
    private EventStore store;

    @BeforeEach
    void setUp()
    {
        store = new InMemoryEventStore();
    }

    @Test
    void computedValuesShouldBeSet()
    {
        Event event = new Event("test event type", JsonValue.EMPTY_JSON_OBJECT);
        ZonedDateTime recorded = event.getDateEventRecorded();
        assertNotNull(recorded, "date event recorded should be set");

        ZonedDateTime now = ZonedDateTime.now();
        Duration since = Duration.between(recorded, now);
        assertTrue(since.toMillis() < 1000, "should be less than a second since event was created");
        
        UUID eventId = event.getId();
        assertNotNull(eventId, "event ID should be set");
    }

    @Test
    void shouldStoreAndRetrieveEvent()
    {
        Event event = new Event("test event type", JsonValue.EMPTY_JSON_OBJECT);

        store.store(event);

        Optional<Event> found = store.findById(event.getId());
        assertTrue(found.isPresent());
        assertSame(event, found.get());
    }

    @Test
    void shouldNotStoreSameEventTwice()
    {
        Event event = new Event("", JsonValue.EMPTY_JSON_OBJECT);
        store.store(event);

        assertThrows(IllegalArgumentException.class, () -> store.store(event));
    }
}
