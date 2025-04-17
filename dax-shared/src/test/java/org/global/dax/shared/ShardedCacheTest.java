package org.global.dax.shared;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ShardedCacheTest {
    private ShardedCache cache;

    @BeforeEach
    void setUp() {
        cache = new ShardedCache();
    }

    @Test
    void testPutAndGet() {
        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1"));
    }

    @Test
    void testOverwriteValue() {
        cache.put("key1", "value1");
        cache.put("key1", "value2");
        assertEquals("value2", cache.get("key1"));
    }

    @Test
    void testRemove() {
        cache.put("key1", "value1");
        assertTrue(cache.remove("key1"));
        assertNull(cache.get("key1"));
    }

    @Test
    void testRemoveNonExistingKey() {
        assertFalse(cache.remove("nonexistent"));
    }

    @Test
    void testGetAllKeys() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");

        List<String> keys = cache.getAllKeys();

        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
        assertTrue(keys.contains("key3"));
        assertEquals(3, keys.size());
    }
}
