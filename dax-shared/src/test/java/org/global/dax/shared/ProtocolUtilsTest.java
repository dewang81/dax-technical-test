package org.global.dax.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.global.dax.shared.ProtocolUtils.handleClientRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProtocolUtilsTest {
    @Test
    void testHeartbeat() {
        ShardedCache cache = new ShardedCache();
        assertEquals("OK", handleClientRequest("HEARTBEAT", cache));
    }

    @Test
    void testAddAndGet() {
        ShardedCache cache = new ShardedCache();
        assertEquals("OK", handleClientRequest("ADD abcd test", cache));
        assertEquals("test", handleClientRequest("GET abcd", cache));
    }

    @Test
    void testAddAndGetAll() {
        ShardedCache cache = new ShardedCache();
        assertEquals("OK", handleClientRequest("ADD abcd test1", cache));
        assertEquals("OK", handleClientRequest("ADD pqrs test2", cache));
        assertEquals("OK", handleClientRequest("ADD klmn test3", cache));
        assertEquals("test1", handleClientRequest("GET abcd", cache));
        assertEquals("test2", handleClientRequest("GET pqrs", cache));
        assertEquals("test3", handleClientRequest("GET klmn", cache));
        assertEquals("klmn,pqrs,abcd", handleClientRequest("GET ALL", cache));
    }

    @Test
    void testDeleteExisting() {
        ShardedCache cache = new ShardedCache();
        cache.put("efgh", "deleteMe");

        assertEquals("OK", handleClientRequest("DELETE efgh", cache));
        assertEquals("", handleClientRequest("GET efgh", cache));
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADD", "ADD abcd", "ADD abcd test test1", "ADD   "})
    void testInvalidAddFormat(String input) {
        ShardedCache cache = new ShardedCache();
        assertEquals("ERROR Invalid ADD format", handleClientRequest(input, cache));
    }

    @Test
    void testInvalidAddKey() {
        ShardedCache cache = new ShardedCache();
        assertEquals("ERROR key too large", handleClientRequest("ADD abcde test", cache));
    }

    @Test
    void testInvalidAddValue() {
        ShardedCache cache = new ShardedCache();
        assertEquals("ERROR value too large", handleClientRequest("ADD abcd " + "A".repeat(2097), cache));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "GET abcd test"})
    void testInvalidGetFormat(String input) {
        ShardedCache cache = new ShardedCache();
        assertEquals("ERROR Invalid GET format", handleClientRequest(input, cache));
    }

    @Test
    void testDeleteNonExisting() {
        ShardedCache cache = new ShardedCache();
        assertEquals("ERROR Invalid key", handleClientRequest("DELETE efgh", cache));
    }

    @ParameterizedTest
    @ValueSource(strings = {"PUT abcd test", "FETCH abcd", "REMOVE abcd"})
    void testUnknownCommands(String input) {
        ShardedCache cache = new ShardedCache();
        assertEquals("ERROR Unknown command", handleClientRequest(input, cache));
    }

    @Test
    void testInvalidInput() {
        ShardedCache cache = new ShardedCache();
        assertEquals("ERROR Invalid input [null]", handleClientRequest(null, cache));
    }
}
