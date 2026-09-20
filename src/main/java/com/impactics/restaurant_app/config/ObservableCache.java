package com.impactics.restaurant_app.config;

import org.springframework.cache.Cache;

import java.util.concurrent.Callable;

public class ObservableCache implements Cache {

    private final Cache delegate;
    private final CacheMetrics cacheMetrics;

    public ObservableCache(Cache delegate, CacheMetrics cacheMetrics) {
        this.delegate = delegate;
        this.cacheMetrics = cacheMetrics;
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public Object getNativeCache() {
        return delegate.getNativeCache();
    }

    @Override
    public ValueWrapper get(Object key) {
        ValueWrapper result = delegate.get(key);
        if (result != null) {
            cacheMetrics.recordHit(getName());
        } else {
            cacheMetrics.recordMiss(getName());
        }
        return result;
    }

    @Override
    public <T> T get(Object key, Class<T> type) {
        return delegate.get(key, type);
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        return delegate.get(key, valueLoader);
    }

    @Override
    public void put(Object key, Object value) {
        delegate.put(key, value);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public void evict(Object key) {
        delegate.evict(key);
    }

    @Override
    public void clear() {
        delegate.clear();
    }
}