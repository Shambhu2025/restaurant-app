package com.impactics.restaurant_app.config;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CacheConfigTest {

    @Test
    void errorHandler_swallowsRedisFailuresInsteadOfPropagating() {
        CacheErrorHandler handler = new CacheConfig().errorHandler();

        Cache cache = mock(Cache.class);
        when(cache.getName()).thenReturn("restaurants");
        RuntimeException redisDown = new RuntimeException("Connection refused");

        // If any of these threw, a Redis outage would turn into a failed request
        assertThatCode(() -> {
            handler.handleCacheGetError(redisDown, cache, "all");
            handler.handleCachePutError(redisDown, cache, "all", "value");
            handler.handleCacheEvictError(redisDown, cache, "all");
            handler.handleCacheClearError(redisDown, cache);
        }).doesNotThrowAnyException();
    }
}