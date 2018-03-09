package at.refugeescode.checkin.config;

import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.stereotype.Component;

import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.util.concurrent.TimeUnit;

/**
 * http://www.ehcache.org/blog/2016/05/18/ehcache3_jsr107_spring.html
 */
@Component
public class CachingSetup implements JCacheManagerCustomizer {
    @Override
    public void customize(CacheManager cacheManager) {
        cacheManager.createCache("dayDurations", new MutableConfiguration<>()
                .setExpiryPolicyFactory(AccessedExpiryPolicy.factoryOf(new Duration(TimeUnit.DAYS, 1)))
                .setStoreByValue(false)
                .setStatisticsEnabled(true));
    }
}