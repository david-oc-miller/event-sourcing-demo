package com.davidocmiller.eventsourcingdemo.web;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.davidocmiller.eventsourcingdemo.store.EventStore;
import com.davidocmiller.eventsourcingdemo.store.InMemoryEventStore;
import com.sun.net.httpserver.HttpServer;

public class EventApi
{
    private EventStore eventStore = new InMemoryEventStore();
    private HttpServer server;

    public static void main(String[] args) throws Exception
    {
        EventApi api = new EventApi();

        Runtime.getRuntime().addShutdownHook(new Thread(api::stop));

        api.run();
    }

    void run() throws IOException
    {
        server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/", new RootCollectionEventHandler(eventStore));
        server.setExecutor(null); // creates a default executor
        server.start();
    }

    public void stop()
    {
        if ( server != null )
        {
            server.stop(0);
        }
    }
}
