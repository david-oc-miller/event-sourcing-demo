package com.davidocmiller.eventsourcingdemo.model;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import jakarta.json.JsonValue;

/**
 * EventTest
 */
public class EventTest
{
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

        List<Event> sources = event.getSourceEvents();
        assertNotNull(sources);
        assertSame(0, sources.size());
    }
}
