package org.global.dax.shared;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Cache {
    private final Map<String, String> store = new ConcurrentHashMap<>();

    public void put(String key, String value) {
        store.put(key, value);
    }

    public String get(String key) {
        return store.get(key);
    }

    public boolean remove(String key) {
        return store.remove(key) != null;
    }

    public List<String> getAllKeys() {
        return List.copyOf(store.keySet());
    }
}
