package com.distrischool.grade.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventProducer {

    private final KafkaTemplate<String, DistriSchoolEvent> kafkaTemplate;

    public void send(String topic, DistriSchoolEvent event) {
        try {
            log.info("Enviando evento para o tópico {}: {}", topic, event.getEventType());
            kafkaTemplate.send(topic, event.getEventId(), event);
            log.info("Evento enviado com sucesso: {}", event.getEventId());
        } catch (Exception e) {
            log.error("Erro ao enviar evento para o tópico {}: {}", topic, e.getMessage(), e);
        }
    }
}

