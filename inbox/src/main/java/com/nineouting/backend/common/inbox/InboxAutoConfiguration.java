package com.nineouting.backend.common.inbox;

import java.time.Clock;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.databind.json.JsonMapper;

/**
 * nineouting.inbox.topics가 설정되면 Kafka 메시지를 inbox에 저장하고 처리기(InboxHandler)에 넘긴다.
 * {@code @AutoConfigurationPackage}로 InboxEvent 엔티티·저장소를 서비스의 JPA 스캔 범위에 넣는다.
 */
@AutoConfiguration(
        beforeName = {
                "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
                "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
                "org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration"})
@AutoConfigurationPackage(basePackageClasses = InboxEvent.class)
@ConditionalOnClass({KafkaListener.class, jakarta.persistence.EntityManager.class})
@ConditionalOnProperty(prefix = "nineouting.inbox", name = "topics")
@EnableConfigurationProperties(InboxProperties.class)
@EnableScheduling
public class InboxAutoConfiguration {

    /** inbox 저장(DB)이 실패하면 메시지를 버리지 않고 5초마다 계속 다시 시도한다 (DB가 돌아오면 이어서 처리). */
    private static final long STORE_RETRY_INTERVAL_MILLIS = 5_000;

    @Bean
    @ConditionalOnMissingBean
    public InboxReceiver inboxReceiver(InboxEventRepository repository, ObjectProvider<JsonMapper> jsonMapper) {
        return new InboxReceiver(repository, jsonMapper.getIfAvailable(() -> JsonMapper.builder().build()), Clock.systemUTC());
    }

    @Bean
    @ConditionalOnMissingBean
    public InboxKafkaListener inboxKafkaListener(InboxReceiver receiver) {
        return new InboxKafkaListener(receiver);
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

    /** Spring Kafka 기본값(10번 재시도 후 버림)을 대신한다. 서비스가 직접 정하면 그것을 쓴다. */
    @Bean
    @ConditionalOnMissingBean(CommonErrorHandler.class)
    public CommonErrorHandler inboxKafkaErrorHandler() {
        return new DefaultErrorHandler(new FixedBackOff(STORE_RETRY_INTERVAL_MILLIS, FixedBackOff.UNLIMITED_ATTEMPTS));
    }
}
