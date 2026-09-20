package com.impactics.restaurant_app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class CacheMetrics {

    private final ConcurrentHashMap<String, AtomicLong> hitCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> missCounts = new ConcurrentHashMap<>();

    public void recordHit(String cacheName) {
        hitCounts.computeIfAbsent(cacheName, k -> new AtomicLong()).incrementAndGet();
        log.info("Cache HIT - cache '{}'", cacheName);
    }

    public void recordMiss(String cacheName) {
        missCounts.computeIfAbsent(cacheName, k -> new AtomicLong()).incrementAndGet();
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> result = new LinkedHashMap<>();
        TreeSet<String> names = new TreeSet<>();
        names.addAll(hitCounts.keySet());
        names.addAll(missCounts.keySet());

        for (String name : names) {
            long hits = hitCounts.getOrDefault(name, new AtomicLong()).get();
            long misses = missCounts.getOrDefault(name, new AtomicLong()).get();
            long total = hits + misses;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("hits", hits);
            entry.put("misses", misses);
            entry.put("hitRatio", total == 0 ? 0.0 : (double) hits / total);
            result.put(name, entry);
        }
        return result;
    }
}