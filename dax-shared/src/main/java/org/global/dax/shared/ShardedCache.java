package org.global.dax.shared;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.global.dax.shared.Constants.NUM_SHARDS;

public class ShardedCache {
    private final List<ConcurrentHashMap<String, String>> shards;

    public ShardedCache() {
        this.shards = new ArrayList<>(NUM_SHARDS);
        for (int i = 0; i < NUM_SHARDS; i++) {
            shards.add(new ConcurrentHashMap<>());
        }
    }

    private ConcurrentHashMap<String, String> getShard(String key) {
        int index = Math.abs(key.hashCode() % NUM_SHARDS);
        return shards.get(index);
    }

    public void put(String key, String value) {
        getShard(key).put(key, value);
    }

    public String get(String key) {
        return getShard(key).get(key);
    }

    public boolean remove(String key) {
        return getShard(key).remove(key) != null;
    }

    public List<String> getAllKeys() {
        return shards.stream()
                .flatMap(shard -> shard.keySet().stream())
                .collect(Collectors.toList());
    }

    public int size() {
        return shards.stream().mapToInt(ConcurrentHashMap::size).sum();
    }

    public Set<String> getKeysForShard(int shardIndex) {
        if (shardIndex < 0 || shardIndex >= NUM_SHARDS) {
            throw new IndexOutOfBoundsException("Invalid shard index");
        }
        return shards.get(shardIndex).keySet();
    }
}
