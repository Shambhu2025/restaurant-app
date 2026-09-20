package com.impactics.restaurant_app.config;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

public class ObservableCacheManager implements CacheManager {

    private final CacheManager delegate;
    private final CacheMetrics cacheMetrics;
    private final ConcurrentHashMap<String, Cache> wrapped = new ConcurrentHashMap<>();

    public ObservableCacheManager(CacheManager delegate, CacheMetrics cacheMetrics) {
        this.delegate = delegate;
        this.cacheMetrics = cacheMetrics;
    }

    @Override
    public Cache getCache(String name) {
        Cache cache = delegate.getCache(name);
        if (cache == null) {
            return null;
        }
        return wrapped.computeIfAbsent(name, n -> new ObservableCache(cache, cacheMetrics));
    }

    @Override
    public Collection<String> getCacheNames() {
        return delegate.getCacheNames();
    }
}