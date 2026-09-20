package com.impactics.restaurant_app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(CacheConfig.class);

    public static final String RESTAURANTS_CACHE = "restaurants";
    public static final String MENU_CACHE = "menu";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory, CacheMetrics cacheMetrics) {

        GenericJacksonJsonRedisSerializer jsonSerializer = GenericJacksonJsonRedisSerializer.builder().build();

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(RESTAURANTS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(MENU_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(5)));

        RedisCacheManager redisCacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();

        return new ObservableCacheManager(redisCacheManager, cacheMetrics);
    }

    @Bean
    public CacheMetricsEndpoint cacheMetricsEndpoint(CacheMetrics cacheMetrics) {
        return new CacheMetricsEndpoint(cacheMetrics);
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis unavailable - falling back to database for cache '{}', key '{}': {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis unavailable - could not cache result for '{}', key '{}': {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis unavailable - could not evict cache '{}', key '{}': {}",
                        cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis unavailable - could not clear cache '{}': {}",
                        cache.getName(), exception.getMessage());
            }
        };
    }
}