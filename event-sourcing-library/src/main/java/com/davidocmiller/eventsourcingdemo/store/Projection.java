package com.davidocmiller.eventsourcingdemo.store;

import com.davidocmiller.eventsourcingdemo.model.Event;

public interface Projection
{
    void notify(Event event);
}
