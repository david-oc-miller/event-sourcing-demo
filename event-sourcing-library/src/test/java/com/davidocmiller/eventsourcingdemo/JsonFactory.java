package com.davidocmiller.eventsourcingdemo;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.StringReader;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

public class JsonFactory
{
    public static JsonObject generateUser()
    {
        String userRegisteredJson = """
                {
                  "event": "userRegistered",
                  "userid": "alice",
                  "firstName": "Alice"
                }
                """;
        JsonObject user;
        try (JsonReader userReader = Json.createReader(new StringReader(userRegisteredJson)))
        {
            user = userReader.readObject();
        }
        assertNotNull(user);
        return user;
    }
}
