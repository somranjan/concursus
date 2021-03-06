package com.opencredo.concursus.domain.events.indexing;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class InMemoryTimestampedTable<K, V, T extends Comparable<T>> {

    public static <K, V, T extends Comparable<T>> InMemoryTimestampedTable<K, V, T> create() {
        return new InMemoryTimestampedTable<>(
                new ConcurrentHashMap<>(),
                InMemoryIndex.create()
        );
    }

    private InMemoryTimestampedTable(Map<K, TimestampedValue<V, T>> tableData, InMemoryIndex<V, K> index) {
        this.tableData = tableData;
        this.index = index;
    }

    private final Map<K, TimestampedValue<V, T>> tableData;
    private final InMemoryIndex<V, K> index;

    public void update(K key, V value, T timestamp) {
        TimestampedValue<V, T> newTv = new TimestampedValue<>(timestamp, value);
        tableData.compute(key, (k, oldTv) -> {
            if (oldTv == null) {
                index.add(value, key);
                return newTv;
            }

            if (oldTv.isBefore(newTv)) {
                index.remove(oldTv.getValue(), key);
                index.add(value, key);
                return newTv;
            }

            return oldTv;
        });
    }

    public Set<K> getIndexed(V value) {
        return index.get(value);
    }
}
