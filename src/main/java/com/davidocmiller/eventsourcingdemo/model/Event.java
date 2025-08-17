package com.davidocmiller.eventsourcingdemo.model;

import java.time.ZonedDateTime;
import java.util.UUID;

import jakarta.json.JsonValue;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.Value;

/**
 * Event
 */
@Getter
@RequiredArgsConstructor
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
    
}
