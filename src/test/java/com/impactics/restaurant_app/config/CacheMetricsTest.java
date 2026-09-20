package com.impactics.restaurant_app.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CacheMetricsTest {

    @Test
    void recordHit_incrementsHitCountForCache() {
        CacheMetrics metrics = new CacheMetrics();

        metrics.recordHit("restaurants");
        metrics.recordHit("restaurants");

        Map<String, Object> snapshot = metrics.snapshot();
        Map<?, ?> restaurantsStats = (Map<?, ?>) snapshot.get("restaurants");

        assertThat(restaurantsStats.get("hits")).isEqualTo(2L);
        assertThat(restaurantsStats.get("misses")).isEqualTo(0L);
    }

    @Test
    void recordMiss_incrementsMissCountForCache() {
        CacheMetrics metrics = new CacheMetrics();

        metrics.recordMiss("menu");

        Map<String, Object> snapshot = metrics.snapshot();
        Map<?, ?> menuStats = (Map<?, ?>) snapshot.get("menu");

        assertThat(menuStats.get("hits")).isEqualTo(0L);
        assertThat(menuStats.get("misses")).isEqualTo(1L);
    }

    @Test
    void snapshot_calculatesCorrectHitRatio() {
        CacheMetrics metrics = new CacheMetrics();

        metrics.recordHit("restaurants");
        metrics.recordHit("restaurants");
        metrics.recordHit("restaurants");
        metrics.recordMiss("restaurants");

        Map<String, Object> snapshot = metrics.snapshot();
        Map<?, ?> restaurantsStats = (Map<?, ?>) snapshot.get("restaurants");

        assertThat(restaurantsStats.get("hits")).isEqualTo(3L);
        assertThat(restaurantsStats.get("misses")).isEqualTo(1L);
        assertThat((Double) restaurantsStats.get("hitRatio")).isEqualTo(0.75);
    }

    @Test
    void snapshot_tracksMultipleCachesIndependently() {
        CacheMetrics metrics = new CacheMetrics();

        metrics.recordHit("restaurants");
        metrics.recordMiss("menu");
        metrics.recordMiss("menu");

        Map<String, Object> snapshot = metrics.snapshot();

        assertThat(snapshot).containsKeys("restaurants", "menu");

        Map<?, ?> restaurantsStats = (Map<?, ?>) snapshot.get("restaurants");
        Map<?, ?> menuStats = (Map<?, ?>) snapshot.get("menu");

        assertThat(restaurantsStats.get("hits")).isEqualTo(1L);
        assertThat(menuStats.get("misses")).isEqualTo(2L);
    }

    @Test
    void snapshot_whenNoActivityRecorded_returnsEmptyMap() {
        CacheMetrics metrics = new CacheMetrics();

        Map<String, Object> snapshot = metrics.snapshot();

        assertThat(snapshot).isEmpty();
    }
}