package com.boon.bank.support;

import jakarta.persistence.EntityManager;
import org.hibernate.Version;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class SmokeDataJpaTest {

    @Autowired
    private EntityManager entityManager;

    @Test
    void dataJpaSliceBootsAgainstTestcontainersPostgres() {
        assertThat(entityManager).as("EntityManager must be wired").isNotNull();

        Object result = entityManager.createNativeQuery("select 1").getSingleResult();
        assertThat(result).as("native query returns a row").isNotNull();
    }

    @Test
    void hibernateMajorVersionIsAtLeastSix() {
        String versionString = Version.getVersionString();
        System.out.println("[Phase 01 smoke] Hibernate version: " + versionString);

        int major = Integer.parseInt(versionString.split("\\.")[0]);
        assertThat(major)
                .as("Hibernate major version must be >= 6 (plan A2 guard); saw %s", versionString)
                .isGreaterThanOrEqualTo(6);
    }
}
