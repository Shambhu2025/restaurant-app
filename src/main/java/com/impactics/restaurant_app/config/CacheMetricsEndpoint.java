package com.impactics.restaurant_app.config;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.Map;

@Endpoint(id = "cachemetrics")
public class CacheMetricsEndpoint {

    private final CacheMetrics cacheMetrics;

    public CacheMetricsEndpoint(CacheMetrics cacheMetrics) {
        this.cacheMetrics = cacheMetrics;
    }

    @ReadOperation
    public Map<String, Object> cacheMetrics() {
        return cacheMetrics.snapshot();
    }
}