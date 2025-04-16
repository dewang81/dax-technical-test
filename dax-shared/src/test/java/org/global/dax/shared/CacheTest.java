package org.global.dax.shared;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CacheTest {
    @Test
    void testCachePutGet() {
        Cache cache = new Cache();
        cache.put("key1", "value1");

        //individual key from cache
        assertEquals("value1", cache.get("key1"));
    }

    @Test
    void testCachePutGetAll() {
        Cache cache = new Cache();
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        cache.put("key4", "value4");

        //individual key from cache
        assertEquals("value1", cache.get("key1"));
        assertEquals("value2", cache.get("key2"));
        assertEquals("value3", cache.get("key3"));
        assertEquals("value4", cache.get("key4"));

        //get all keys
        assertEquals(List.of("key1", "key2", "key3", "key4"), cache.getAllKeys());
    }

    @Test
    void testCacheDelete() {
        Cache cache = new Cache();
        cache.put("key2", "value2");

        //existing key removal
        assertTrue(cache.remove("key2"));
        assertNull(cache.get("key2"));

        //non-existing key removal
        assertFalse(cache.remove("key3"));
        assertNull(cache.get("key3"));
    }
}
