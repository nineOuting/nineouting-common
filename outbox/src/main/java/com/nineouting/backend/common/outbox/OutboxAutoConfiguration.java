package com.nineouting.backend.common.outbox;

import io.micrometer.tracing.Tracer;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.json.JsonMapper;

/**
 * 의존성만 추가하면 outbox가 동작한다.
 * {@code @AutoConfigurationPackage}로 OutboxEvent 엔티티와 저장소를 서비스의 JPA 스캔 범위에 넣으므로
 * 서비스가 따로 {@code @EntityScan}을 쓰지 않아도 된다. 그래서 JPA 자동 설정보다 먼저 처리돼야 한다.
 */
@AutoConfiguration(
        beforeName = {
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
                "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"},
        afterName = "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration")
@AutoConfigurationPackage(basePackageClasses = OutboxEvent.class)
@ConditionalOnClass({KafkaTemplate.class, jakarta.persistence.EntityManager.class})
@ConditionalOnProperty(prefix = "nineouting.outbox", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(OutboxProperties.class)
@EnableScheduling
public class OutboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OutboxEventWriter outboxEventWriter(OutboxEventRepository repository, ObjectProvider<JsonMapper> jsonMapper,
                                               ObjectProvider<Tracer> tracer, OutboxProperties properties,
                                               Environment environment) {
        String source = properties.source() != null
                ? properties.source()
                : environment.getProperty("spring.application.name", "unknown");
        return new OutboxEventWriter(repository, jsonMapper.getIfAvailable(() -> JsonMapper.builder().build()),
                tracer, source, Clock.systemUTC());
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxSender outboxSender(KafkaTemplate<String, String> kafkaTemplate) {
        return new KafkaOutboxSender(kafkaTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxRelay outboxRelay(OutboxEventRepository repository, OutboxSender sender, OutboxProperties properties) {
        return new OutboxRelay(repository, sender, properties, Clock.systemUTC());
    }

    @Bean
    @ConditionalOnMissingBean
    public OutboxCleaner outboxCleaner(OutboxEventRepository repository, OutboxProperties properties) {
        return new OutboxCleaner(repository, properties, Clock.systemUTC());
    }
}
