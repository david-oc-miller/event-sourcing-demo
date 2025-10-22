package com.optivem.atddaccelerator.template.systemtest.e2etests;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

class ApiE2eTest
{

    @Test
    void createUserCommand() throws Exception
    {
        String createUserCommand = """
                {
                  "command": "registerUser",
                  "userId": "alice",
                  "firstName": "Alice"
                }
                """;

        HttpClient client = HttpClient.newBuilder().build();
        HttpRequest addEventRequest = HttpRequest.newBuilder().uri(URI.create("http://localhost:8000"))
                .PUT(HttpRequest.BodyPublishers.ofString(createUserCommand)).build();
        HttpResponse<String> addEventResponse = client.send(addEventRequest, BodyHandlers.ofString());

        String body = addEventResponse.body();

        Pattern regexp = Pattern.compile("event id: ([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})");
        Matcher matcher = regexp.matcher(body);
        assertTrue(matcher.matches());
        String eventId = matcher.group(1);
        System.out.println(eventId);

        HttpRequest getEventRequest = HttpRequest.newBuilder().uri(URI.create("http://localhost:8000?eventId=" + eventId)).build();
        HttpResponse<String> getEventResponse = client.send(getEventRequest, BodyHandlers.ofString());

        String getBody = getEventResponse.body();

        try (JsonReader eventReader = Json.createReader(new StringReader(getBody)))
        {
            JsonObject eventData = eventReader.readObject();
            assertEquals("alice", eventData.getString("userId"));
            assertEquals("Alice", eventData.getString("firstName"));
        }
    }
}