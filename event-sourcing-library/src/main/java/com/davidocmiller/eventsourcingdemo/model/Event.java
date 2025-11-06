package com.davidocmiller.eventsourcingdemo.model;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

/**
 * Event
 */
@Getter
@Value
@EqualsAndHashCode
@ToString()
public class Event
{

    @NonNull
    private String eventType;

    private ZonedDateTime dateEventRecorded = ZonedDateTime.now();

    private UUID id = UUID.randomUUID();

    @NonNull
    @ToString.Exclude
    private JsonValue body;

    private List<Event> sourceEvents;

    public Event(String eventType, JsonObject body)
    {
        this(eventType, body, Collections.emptyList());
    }

    public Event(String eventType, JsonObject body, List<Event> sourceEvents)
    {
        this.eventType = eventType;
        this.body = body;
        this.sourceEvents = sourceEvents;
    }

    public List<Event> getSourceEvents()
    {
        return Collections.unmodifiableList(sourceEvents);
    }

}
