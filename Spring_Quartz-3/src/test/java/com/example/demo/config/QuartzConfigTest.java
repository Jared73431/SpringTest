package com.example.demo.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

class QuartzConfigTest {

    private Properties loadQuartzProperties(MockEnvironment environment) throws Exception {
        QuartzConfig config = new QuartzConfig();
        // QuartzConfig 使用 Field Injection，單元測試只能用反射注入
        ReflectionTestUtils.setField(config, "environment", environment);
        return config.quartzProperties();
    }

    @Test
    void quartzProperties_shouldUseDefaultCredentials_whenEnvironmentVariablesNotSet() throws Exception {
        Properties properties = loadQuartzProperties(new MockEnvironment());

        assertThat(properties.getProperty("org.quartz.dataSource.myDS.user")).isEqualTo("postgres");
        assertThat(properties.getProperty("org.quartz.dataSource.myDS.password")).isEqualTo("postgres");
    }

    @Test
    void quartzProperties_shouldUseEnvironmentValue_whenEnvironmentVariablesSet() throws Exception {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("DB_USERNAME", "quartz_user")
                .withProperty("DB_PASSWORD", "secret");

        Properties properties = loadQuartzProperties(environment);

        assertThat(properties.getProperty("org.quartz.dataSource.myDS.user")).isEqualTo("quartz_user");
        assertThat(properties.getProperty("org.quartz.dataSource.myDS.password")).isEqualTo("secret");
    }

    @Test
    void quartzProperties_shouldKeepOtherValuesUnchanged_whenNoPlaceholder() throws Exception {
        Properties properties = loadQuartzProperties(new MockEnvironment());

        assertThat(properties.getProperty("org.quartz.dataSource.myDS.URL")).isEqualTo("jdbc:postgresql://localhost:5432/quartz");
        assertThat(properties.getProperty("org.quartz.threadPool.threadCount")).isEqualTo("25");
        assertThat(properties.getProperty("org.quartz.jobStore.tablePrefix")).isEqualTo("qrtz_");
    }
}
