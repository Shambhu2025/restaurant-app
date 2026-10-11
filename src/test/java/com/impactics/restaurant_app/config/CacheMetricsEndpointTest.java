package com.impactics.restaurant_app.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CacheMetricsEndpointTest {

    @Test
    void cacheMetrics_returnsCurrentHitsAndMisses() {
        CacheMetrics metrics = new CacheMetrics();
        metrics.recordHit("restaurants");
        metrics.recordHit("restaurants");
        metrics.recordMiss("restaurants");

        Map<String, Object> result = new CacheMetricsEndpoint(metrics).cacheMetrics();

        assertThat(result).containsKey("restaurants");
        Map<?, ?> stats = (Map<?, ?>) result.get("restaurants");
        assertThat(stats.get("hits")).isEqualTo(2L);
        assertThat(stats.get("misses")).isEqualTo(1L);
    }
}