package com.davidocmiller.eventsourcingdemo.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.davidocmiller.eventsourcingdemo.model.Event;
import com.davidocmiller.eventsourcingdemo.store.EventStore;
import com.davidocmiller.eventsourcingdemo.store.InMemoryEventStore;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

public class UserProjectionTest
{
    private EventStore store;
    private UserProjection userProjection;

    @BeforeEach
    void setUp()
    {
        store = new InMemoryEventStore();
        userProjection = new UserProjection();
        store.register(userProjection);
    }

    @Test
    void after_register_user_can_find_user() throws Exception
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

        Event userRegistered = new Event("userRegistered", user);
        store.store(userRegistered);

        Optional<User> found = userProjection.findById("alice");
        assertTrue(found.isPresent());
        assertEquals("alice", found.get().getUserId());
        assertEquals("Alice", found.get().getFirstName());
    }

}
