package com.nineouting.backend.common.outbox;

import io.micrometer.tracing.Tracer;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.SnsClientBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * 의존성만 추가하면 outbox가 동작한다.
 * {@code @AutoConfigurationPackage}로 OutboxEvent 엔티티와 저장소를 서비스의 JPA 스캔 범위에 넣으므로
 * 서비스가 따로 {@code @EntityScan}을 쓰지 않아도 된다. 그래서 JPA 자동 설정보다 먼저 처리돼야 한다.
 * 이벤트는 SNS로 보낸다 ({@link SnsOutboxSender}).
 */
@AutoConfiguration(
        beforeName = {
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
                "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration"})
@AutoConfigurationPackage(basePackageClasses = OutboxEvent.class)
@ConditionalOnClass(jakarta.persistence.EntityManager.class)
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
    public OutboxRelay outboxRelay(OutboxEventRepository repository, OutboxSender sender, OutboxProperties properties) {
        return new OutboxRelay(repository, sender, properties, Clock.systemUTC());
    }

    /**
     * 리전·주소·자격 증명은 {@code nineouting.messaging.region}·{@code endpoint}·{@code access-key}/{@code secret-key}가
     * 있으면 쓰고(로컬 LocalStack), 없으면 AWS SDK 기본 방식으로 읽는다 (EKS는 IRSA로 파드에 준 IAM 역할).
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(SnsClient.class)
    static class SnsSenderConfiguration {

        /** 응답이 이보다 늦으면 실패로 보고 다시 보낸다 */
        private static final Duration SEND_TIMEOUT = Duration.ofSeconds(10);

        @Bean(destroyMethod = "close")
        @ConditionalOnMissingBean
        SnsClient snsClient(Environment environment) {
            SnsClientBuilder builder = SnsClient.builder()
                    .overrideConfiguration(config -> config.apiCallTimeout(SEND_TIMEOUT));
            String region = environment.getProperty("nineouting.messaging.region");
            if (StringUtils.hasText(region)) {
                builder.region(Region.of(region));
            }
            String endpoint = environment.getProperty("nineouting.messaging.endpoint");
            if (StringUtils.hasText(endpoint)) {
                builder.endpointOverride(URI.create(endpoint));
            }
            String accessKey = environment.getProperty("nineouting.messaging.access-key");
            String secretKey = environment.getProperty("nineouting.messaging.secret-key");
            if (StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey)) {
                builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
            }
            return builder.build();
        }

        @Bean
        @ConditionalOnMissingBean
        OutboxSender outboxSender(SnsClient snsClient, OutboxProperties properties) {
            return new SnsOutboxSender(snsClient, properties.topicPrefix(), properties.fifo());
        }
    }
}
