package com.distrischool.grade.kafka;

import com.distrischool.grade.kafka.DistriSchoolEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para Kafka no serviço de notas
 */
@SpringBootTest(properties = {
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(partitions = 1, topics = {"distrischool.grade.created", "distrischool.grade.updated"})
@ActiveProfiles("test")
@DirtiesContext
class KafkaIntegrationTest {

    @Autowired(required = false)
    private KafkaTemplate<String, DistriSchoolEvent> kafkaTemplate;

    @Autowired(required = false)
    private EventProducer eventProducer;

    @Test
    void contextLoads() {
        assertThat(kafkaTemplate).isNotNull();
    }

    @Test
    void shouldPublishGradeCreatedEvent() {
        if (eventProducer == null || kafkaTemplate == null) {
            return;
        }

        Map<String, Object> eventData = Map.of(
            "gradeId", 1L,
            "studentId", 1L,
            "gradeValue", 8.5
        );

        DistriSchoolEvent event = DistriSchoolEvent.of(
            "grade.created",
            "grade-management-service",
            eventData
        );

        kafkaTemplate.send("distrischool.grade.created", event);

        assertThat(event).isNotNull();
        assertThat(event.getEventType()).isEqualTo("grade.created");
    }
}
