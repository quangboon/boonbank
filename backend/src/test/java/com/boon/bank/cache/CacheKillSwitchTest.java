package com.boon.bank.cache;

import com.boon.bank.support.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.cache.type=none")
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class CacheKillSwitchTest {

    @Autowired CacheManager cacheManager;

    @Test
    void cacheManager_isNoOp_whenSpringCacheTypeIsNone() {
        assertThat(cacheManager).isInstanceOf(NoOpCacheManager.class);
    }
}
