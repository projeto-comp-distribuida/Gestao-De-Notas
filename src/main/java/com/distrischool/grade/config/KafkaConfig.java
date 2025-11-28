package com.distrischool.grade.config;

import com.distrischool.grade.kafka.DistriSchoolEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuração dos tópicos Kafka para o Grade Management Service
 */
@Configuration
public class KafkaConfig {

    @Value("${microservice.kafka.topics.grade-created:distrischool.grade.created}")
    private String gradeCreatedTopic;

    @Value("${microservice.kafka.topics.grade-updated:distrischool.grade.updated}")
    private String gradeUpdatedTopic;

    @Value("${microservice.kafka.topics.grade-deleted:distrischool.grade.deleted}")
    private String gradeDeletedTopic;

    @Value("${microservice.kafka.topics.evaluation-created:distrischool.evaluation.created}")
    private String evaluationCreatedTopic;

    @Value("${microservice.kafka.topics.assessment-finalized:distrischool.assessment.finalized}")
    private String assessmentFinalizedTopic;

    @Bean
    public NewTopic gradeCreatedTopic() {
        return TopicBuilder.name(gradeCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic gradeUpdatedTopic() {
        return TopicBuilder.name(gradeUpdatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic gradeDeletedTopic() {
        return TopicBuilder.name(gradeDeletedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic evaluationCreatedTopic() {
        return TopicBuilder.name(evaluationCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic assessmentFinalizedTopic() {
        return TopicBuilder.name(assessmentFinalizedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Configuração do ConsumerFactory com tratamento de erros de deserialização
     */
    @Bean
    public ConsumerFactory<String, DistriSchoolEvent> consumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "grade-management-service-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        
        // Configuração do ErrorHandlingDeserializer
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        
        // Configuração do JsonDeserializer
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, DistriSchoolEvent.class.getName());
        props.put(JsonDeserializer.REMOVE_TYPE_INFO_HEADERS, true);

        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Configuração do KafkaListenerContainerFactory com tratamento de erros
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DistriSchoolEvent> kafkaListenerContainerFactory(
            ConsumerFactory<String, DistriSchoolEvent> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, DistriSchoolEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        
        // Configuração de tratamento de erros
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new FixedBackOff(1000L, 3L) // Retry 3 vezes com intervalo de 1 segundo
        );
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            System.err.println("Failed record: " + record + ", attempt: " + deliveryAttempt);
        });
        factory.setCommonErrorHandler(errorHandler);
        
        return factory;
    }
}

