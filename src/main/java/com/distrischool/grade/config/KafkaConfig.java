package com.distrischool.grade.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

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
}

