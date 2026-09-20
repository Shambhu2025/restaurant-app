package com.impactics.restaurant_app.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;

import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObservableCacheTest {

    @Mock
    private Cache delegate;

    @Mock
    private CacheMetrics cacheMetrics;

    private ObservableCache observableCache;

    @BeforeEach
    void setUp() {
        lenient().when(delegate.getName()).thenReturn("testCache");
        observableCache = new ObservableCache(delegate, cacheMetrics);
    }

    @Test
    void get_whenValuePresent_recordsHitAndReturnsValue() {
        Cache.ValueWrapper wrapper = new SimpleValueWrapper("cachedValue");
        when(delegate.get("key1")).thenReturn(wrapper);

        Cache.ValueWrapper result = observableCache.get("key1");

        assertThat(result.get()).isEqualTo("cachedValue");
        verify(cacheMetrics).recordHit("testCache");
        verify(cacheMetrics, never()).recordMiss(anyString());
    }

    @Test
    void get_whenValueAbsent_recordsMissAndReturnsNull() {
        when(delegate.get("missingKey")).thenReturn(null);

        Cache.ValueWrapper result = observableCache.get("missingKey");

        assertThat(result).isNull();
        verify(cacheMetrics).recordMiss("testCache");
        verify(cacheMetrics, never()).recordHit(anyString());
    }

    @Test
    void getName_delegatesToUnderlyingCache() {
        assertThat(observableCache.getName()).isEqualTo("testCache");
    }

    @Test
    void getNativeCache_delegatesToUnderlyingCache() {
        Object nativeCache = new Object();
        when(delegate.getNativeCache()).thenReturn(nativeCache);

        assertThat(observableCache.getNativeCache()).isSameAs(nativeCache);
    }

    @Test
    void getWithType_delegatesToUnderlyingCache() {
        when(delegate.get("key1", String.class)).thenReturn("value");

        String result = observableCache.get("key1", String.class);

        assertThat(result).isEqualTo("value");
        verify(delegate).get("key1", String.class);
    }

    @Test
    void getWithCallable_delegatesToUnderlyingCache() {
        Callable<String> loader = () -> "loadedValue";
        when(delegate.get(eq("key1"), any(Callable.class))).thenReturn("loadedValue");

        String result = observableCache.get("key1", loader);

        assertThat(result).isEqualTo("loadedValue");
    }

    @Test
    void put_delegatesToUnderlyingCache() {
        observableCache.put("key1", "value1");

        verify(delegate).put("key1", "value1");
    }

    @Test
    void putIfAbsent_delegatesToUnderlyingCache() {
        Cache.ValueWrapper wrapper = new SimpleValueWrapper("existingValue");
        when(delegate.putIfAbsent("key1", "value1")).thenReturn(wrapper);

        Cache.ValueWrapper result = observableCache.putIfAbsent("key1", "value1");

        assertThat(result.get()).isEqualTo("existingValue");
    }

    @Test
    void evict_delegatesToUnderlyingCache() {
        observableCache.evict("key1");

        verify(delegate).evict("key1");
    }

    @Test
    void clear_delegatesToUnderlyingCache() {
        observableCache.clear();

        verify(delegate).clear();
    }
}