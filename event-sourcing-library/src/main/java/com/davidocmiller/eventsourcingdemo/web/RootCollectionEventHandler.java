package com.davidocmiller.eventsourcingdemo.web;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

import com.davidocmiller.eventsourcingdemo.model.Event;
import com.davidocmiller.eventsourcingdemo.store.EventStore;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.stream.JsonGenerator;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class RootCollectionEventHandler implements HttpHandler
{
    private final EventStore eventStore;

    @Override
    public void handle(HttpExchange exchange) throws IOException
    {
        System.out.println(String.format("Got a request: %s", exchange.getRequestMethod()));
        switch ( exchange.getRequestMethod() )
        {
            case "PUT" -> handlePut(exchange);
            case "GET" -> handleGet(exchange);
            default -> throw new IllegalStateException(String.format("Unhandled action: %s", exchange.getRequestMethod()));
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException
    {
        URI uri = exchange.getRequestURI();
        String query = uri.getQuery();
        String eventId = query.substring(query.indexOf("=") + 1);

        Optional<Event> maybeEvent = eventStore.findById(UUID.fromString(eventId));
        if ( maybeEvent.isPresent() )
        {
            Event event = maybeEvent.get();
            StringWriter writer = new StringWriter();
            JsonGenerator generator = Json.createGenerator(writer);
            generator.write(event.getBody());
            generator.close();
            String response = writer.toString();
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody())
            {
                os.write(response.getBytes());
            }
        }
    }

    public void handlePut(HttpExchange exchange) throws IOException
    {
        try (JsonReader userReader = Json.createReader(new InputStreamReader(exchange.getRequestBody())))
        {
            JsonObject body = userReader.readObject();
            String command = body.getString("command");
            Event event = switch (command)
            {
                case "registerUser" -> registerUser(body);
                default -> throw new IllegalStateException(String.format("Unhandled command: %s", command));
            };

            String response = String.format("event id: %s", event.getId());
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody())
            {
                os.write(response.getBytes());
            }
        }
    }

    private Event registerUser(JsonObject body)
    {
        if ( !body.containsKey("userId") ||
                !body.containsKey("firstName") )
        {
            throw new IllegalArgumentException("registerUser event must have firstName and userId");
        }
        JsonObject eventData = Json.createObjectBuilder()
                .add("event", "userRegistered")
                .add("userId", body.getString("userId"))
                .add("firstName", body.getString("firstName")).build();
        Event userRegistered = new Event("userRegistered", eventData);
        eventStore.store(userRegistered);

        return userRegistered;

    }
}
