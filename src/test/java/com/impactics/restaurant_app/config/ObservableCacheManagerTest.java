package com.impactics.restaurant_app.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObservableCacheManagerTest {

    @Mock
    private CacheManager delegate;

    @Mock
    private CacheMetrics cacheMetrics;

    @Mock
    private Cache realCache;

    @Test
    void getCache_wrapsRealCacheInObservableCache() {
        when(delegate.getCache("restaurants")).thenReturn(realCache);

        ObservableCacheManager manager = new ObservableCacheManager(delegate, cacheMetrics);
        Cache result = manager.getCache("restaurants");

        assertThat(result).isInstanceOf(ObservableCache.class);
    }

    @Test
    void getCache_whenCalledTwice_returnsSameWrappedInstance() {
        when(delegate.getCache("restaurants")).thenReturn(realCache);

        ObservableCacheManager manager = new ObservableCacheManager(delegate, cacheMetrics);
        Cache first = manager.getCache("restaurants");
        Cache second = manager.getCache("restaurants");

        assertThat(first).isSameAs(second);
    }

    @Test
    void getCache_whenDelegateReturnsNull_returnsNull() {
        when(delegate.getCache("nonexistent")).thenReturn(null);

        ObservableCacheManager manager = new ObservableCacheManager(delegate, cacheMetrics);
        Cache result = manager.getCache("nonexistent");

        assertThat(result).isNull();
    }

    @Test
    void getCacheNames_delegatesToUnderlyingManager() {
        when(delegate.getCacheNames()).thenReturn(List.of("restaurants", "menu"));

        ObservableCacheManager manager = new ObservableCacheManager(delegate, cacheMetrics);

        assertThat(manager.getCacheNames()).containsExactly("restaurants", "menu");
    }
}