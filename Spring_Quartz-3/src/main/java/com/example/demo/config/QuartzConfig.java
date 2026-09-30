package com.example.demo.config;

import java.io.IOException;
import java.util.Properties;

import org.quartz.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import com.example.demo.component.MyJobFactory;

@Configuration
public class QuartzConfig {

    private final static Logger logger = LoggerFactory.getLogger(QuartzConfig.class);

    @Autowired
    private MyJobFactory myJobFactory;

    @Autowired
    private Environment environment;

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean() {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();
        try {
            schedulerFactoryBean.setSchedulerName("MySchedulerFactory");
            schedulerFactoryBean.setQuartzProperties(quartzProperties());
            schedulerFactoryBean.setJobFactory(myJobFactory);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return schedulerFactoryBean;
    }

    /**
     * 从配置文件中获取对应的配置
     *
     * @return
     * @throws IOException
     */
    @Bean
    public Properties quartzProperties() throws IOException {
        PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
        propertiesFactoryBean.setLocation(new ClassPathResource("/application.properties"));
        propertiesFactoryBean.afterPropertiesSet();

        // PropertiesFactoryBean 只讀原始檔案，不會解析 ${...}，交給 Spring Environment 解析（例如 ${DB_PASSWORD:postgres}）
        Properties properties = propertiesFactoryBean.getObject();
        properties.replaceAll((key, value) -> environment.resolvePlaceholders((String) value));
        return properties;
    }

    /**
     * 创建schedule
     *
     * @return
     * @throws IOException
     */
    @Bean(name = "scheduler")
    public Scheduler scheduler() {
        return schedulerFactoryBean().getScheduler();
    }
}
