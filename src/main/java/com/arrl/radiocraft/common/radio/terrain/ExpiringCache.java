package com.arrl.radiocraft.common.radio.terrain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Results kept for a while, safe on any thread: terrain is worked out again once an entry is older than the time to
 * live (0 keeps nothing). Past {@code maxSize} entries everything is dropped at once, which only costs recalculation.
 */
public final class ExpiringCache<K, V> {

    private record Entry<V>(V value, long expiresAt) {}

    private final Map<K, Entry<V>> entries = new ConcurrentHashMap<>();
    private final int maxSize;

    public ExpiringCache(int maxSize) {
        this.maxSize = maxSize;
    }

    /** The value kept for {@code key}, or a new one from {@code compute} kept for {@code ttlNanos}. */
    public V get(K key, long ttlNanos, Function<K, V> compute) {
        return get(key, System.nanoTime(), ttlNanos, compute);
    }

    /**
     * As {@link #get(Object, long, Function)}; an entry past its time that {@code stillGood} accepts is kept for
     * another {@code ttlNanos} instead of being worked out again.
     */
    public V get(K key, long ttlNanos, Function<K, V> compute, Predicate<V> stillGood) {
        return get(key, System.nanoTime(), ttlNanos, compute, stillGood);
    }

    /** As {@link #get(Object, long, Function)} at a given time (nanoseconds, any origin), for tests. */
    public V get(K key, long now, long ttlNanos, Function<K, V> compute) {
        return get(key, now, ttlNanos, compute, value -> false);
    }

    public V get(K key, long now, long ttlNanos, Function<K, V> compute, Predicate<V> stillGood) {
        Entry<V> entry = entries.get(key);
        if(entry != null && entry.expiresAt() > now)
            return entry.value();
        if(entry != null && ttlNanos > 0 && stillGood.test(entry.value())) {
            entries.put(key, new Entry<>(entry.value(), now + ttlNanos));
            return entry.value();
        }
        V value = compute.apply(key);
        if(ttlNanos > 0) {
            if(entries.size() > maxSize)
                entries.clear();
            entries.put(key, new Entry<>(value, now + ttlNanos));
        }
        return value;
    }

    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }
}
