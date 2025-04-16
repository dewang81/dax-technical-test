package org.global.dax.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProtocolHandlerTest {
    @Test
    void testHeartbeat() {
        Cache cache = new Cache();
        assertEquals("OK", ProtocolHandler.handleClientRequest("HEARTBEAT", cache));
    }

    @Test
    void testAddAndGet() {
        Cache cache = new Cache();
        assertEquals("OK", ProtocolHandler.handleClientRequest("ADD abcd test", cache));
        assertEquals("test", ProtocolHandler.handleClientRequest("GET abcd", cache));
    }

    @Test
    void testAddAndGetAll() {
        Cache cache = new Cache();
        assertEquals("OK", ProtocolHandler.handleClientRequest("ADD abcd test1", cache));
        assertEquals("OK", ProtocolHandler.handleClientRequest("ADD pqrs test2", cache));
        assertEquals("OK", ProtocolHandler.handleClientRequest("ADD klmn test3", cache));
        assertEquals("test1", ProtocolHandler.handleClientRequest("GET abcd", cache));
        assertEquals("test2", ProtocolHandler.handleClientRequest("GET pqrs", cache));
        assertEquals("test3", ProtocolHandler.handleClientRequest("GET klmn", cache));
        assertEquals("klmn,pqrs,abcd", ProtocolHandler.handleClientRequest("GET ALL", cache));
    }

    @Test
    void testDeleteExisting() {
        Cache cache = new Cache();
        cache.put("efgh", "deleteMe");

        assertEquals("OK", ProtocolHandler.handleClientRequest("DELETE efgh", cache));
        assertEquals("", ProtocolHandler.handleClientRequest("GET efgh", cache));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADD", "ADD abcd", "ADD abcd test test1", "ADD   "})
    void testInvalidAddFormat(String input) {
        Cache cache = new Cache();
        assertEquals("ERROR Invalid ADD format", ProtocolHandler.handleClientRequest(input, cache));
    }

    @Test
    void testInvalidAddKey() {
        Cache cache = new Cache();
        assertEquals("ERROR key too large", ProtocolHandler.handleClientRequest("ADD abcde test", cache));
    }

    @Test
    void testInvalidAddValue() {
        Cache cache = new Cache();
        assertEquals("ERROR value too large", ProtocolHandler.handleClientRequest("ADD abcd " + "A".repeat(2097), cache));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "GET abcd test"})
    void testInvalidGetFormat(String input) {
        Cache cache = new Cache();
        assertEquals("ERROR Invalid GET format", ProtocolHandler.handleClientRequest(input, cache));
    }

    @Test
    void testDeleteNonExisting() {
        Cache cache = new Cache();
        assertEquals("ERROR Invalid key", ProtocolHandler.handleClientRequest("DELETE efgh", cache));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUT abcd test", "FETCH abcd", "REMOVE abcd"})
    void testUnknownCommands(String input) {
        Cache cache = new Cache();
        assertEquals("ERROR Unknown command", ProtocolHandler.handleClientRequest(input, cache));
    }

    @Test
    void testInvalidInput() {
        Cache cache = new Cache();
        assertEquals("ERROR Invalid input [null]", ProtocolHandler.handleClientRequest(null, cache));
    }
}
