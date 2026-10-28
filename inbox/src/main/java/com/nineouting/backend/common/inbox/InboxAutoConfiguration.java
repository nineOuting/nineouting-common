package com.nineouting.backend.common.inbox;

import java.net.URI;
import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.util.backoff.FixedBackOff;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import tools.jackson.databind.json.JsonMapper;

/**
 * nineouting.inbox.topics(Kafka) 또는 nineouting.inbox.queue(SQS)가 설정되면 받은 메시지를 inbox에 저장하고
 * 처리기(InboxHandler)에 넘긴다.
 * {@code @AutoConfigurationPackage}로 InboxEvent 엔티티·저장소를 서비스의 JPA 스캔 범위에 넣는다.
 */
@AutoConfiguration(
        beforeName = {
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
                "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
                "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration"})
@AutoConfigurationPackage(basePackageClasses = InboxEvent.class)
@ConditionalOnClass(jakarta.persistence.EntityManager.class)
@Conditional(InboxAutoConfiguration.SourceConfigured.class)
@EnableConfigurationProperties(InboxProperties.class)
@EnableScheduling
public class InboxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public InboxReceiver inboxReceiver(InboxEventRepository repository, ObjectProvider<JsonMapper> jsonMapper) {
        return new InboxReceiver(repository, jsonMapper.getIfAvailable(() -> JsonMapper.builder().build()), Clock.systemUTC());
    }

    @Bean
    @ConditionalOnMissingBean
    public InboxProcessor inboxProcessor(InboxEventRepository repository, List<InboxHandler> handlers,
                                         PlatformTransactionManager transactionManager, InboxProperties properties) {
        return new InboxProcessor(repository, handlers, new TransactionTemplate(transactionManager), properties,
                Clock.systemUTC());
    }

    @Bean
    @ConditionalOnMissingBean
    public InboxCleaner inboxCleaner(InboxEventRepository repository, InboxProperties properties) {
        return new InboxCleaner(repository, properties, Clock.systemUTC());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(KafkaListener.class)
    @ConditionalOnProperty(prefix = "nineouting.inbox", name = "topics")
    static class KafkaInboxConfiguration {

        /** inbox 저장(DB)이 실패하면 메시지를 버리지 않고 5초마다 계속 다시 시도한다 (DB가 돌아오면 이어서 처리). */
        private static final long STORE_RETRY_INTERVAL_MILLIS = 5_000;

        @Bean
        @ConditionalOnMissingBean
        InboxKafkaListener inboxKafkaListener(InboxReceiver receiver) {
            return new InboxKafkaListener(receiver);
        }

        /** Spring Kafka 기본값(10번 재시도 후 버림)을 대신한다. 서비스가 직접 정하면 그것을 쓴다. */
        @Bean
        @ConditionalOnMissingBean(CommonErrorHandler.class)
        CommonErrorHandler inboxKafkaErrorHandler() {
            return new DefaultErrorHandler(new FixedBackOff(STORE_RETRY_INTERVAL_MILLIS, FixedBackOff.UNLIMITED_ATTEMPTS));
        }
    }

    /**
     * 리전·주소·자격 증명은 {@code nineouting.messaging.region}·{@code endpoint}·{@code access-key}/{@code secret-key}가
     * 있으면 쓰고(로컬 LocalStack), 없으면 AWS SDK 기본 방식으로 읽는다 (EKS는 IRSA로 파드에 준 IAM 역할).
     * 저장에 실패한 메시지는 큐의 보이지 않는 시간이 지나 다시 오고, 여러 번 실패하면 큐 설정대로 DLQ로 간다.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(SqsClient.class)
    @ConditionalOnProperty(prefix = "nineouting.inbox", name = "queue")
    static class SqsInboxConfiguration {

        @Bean(destroyMethod = "close")
        @ConditionalOnMissingBean
        SqsClient sqsClient(Environment environment) {
            SqsClientBuilder builder = SqsClient.builder();
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
        SqsInboxListener sqsInboxListener(SqsClient sqsClient, InboxReceiver receiver, InboxProperties properties) {
            return new SqsInboxListener(sqsClient, receiver, properties.queue());
        }
    }

    /** topics나 queue 중 하나라도 있으면 켠다 */
    static class SourceConfigured extends AnyNestedCondition {

        SourceConfigured() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty(prefix = "nineouting.inbox", name = "topics")
        static class Topics {
        }

        @ConditionalOnProperty(prefix = "nineouting.inbox", name = "queue")
        static class Queue {
        }
    }
}
