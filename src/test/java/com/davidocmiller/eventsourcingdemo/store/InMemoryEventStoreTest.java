package com.davidocmiller.eventsourcingdemo.store;

import org.junit.jupiter.api.BeforeEach;

public class InMemoryEventStoreTest extends EventStoreTest
{
    @BeforeEach
    void setUp()
    {
        setEventStore(new InMemoryEventStore());
    }
}
