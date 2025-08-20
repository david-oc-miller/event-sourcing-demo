package com.davidocmiller.eventsourcingdemo.store;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

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

    @Test
    void shouldStoreAndRetrieveRelatedEvents()
    {
        Event root = new Event("", JsonValue.EMPTY_JSON_OBJECT);
        store.store(root);

        Event level2 = new Event("level2", JsonValue.EMPTY_JSON_OBJECT, List.of(root));
        store.store(level2);

        List<Event> downstream = store.downstreamEvents(root);
        assertNotNull(downstream, "Should have list of downstream events");
        assertSame(1, downstream.size(), "Should have one downstream event");
        assertSame(level2, downstream.get(0));
    }
}
