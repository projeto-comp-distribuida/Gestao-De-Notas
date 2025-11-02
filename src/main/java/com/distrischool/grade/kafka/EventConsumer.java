package com.distrischool.grade.kafka;

import com.distrischool.grade.kafka.DistriSchoolEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumer de eventos Kafka do DistriSchool
 * Escuta eventos de outros microserviços (student, teacher, etc.)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventConsumer {

    /**
     * Escuta eventos de estudantes criados
     * Quando um estudante é criado, podemos inicializar estruturas relacionadas
     */
    @KafkaListener(
        topics = "${microservice.kafka.topics.student-created:distrischool.student.created}",
        groupId = "${spring.application.name}-group"
    )
    public void consumeStudentCreatedEvent(DistriSchoolEvent event) {
        log.info("Evento recebido - Student Created: {}", event.getEventId());
        
        try {
            Map<String, Object> data = event.getData();
            if (data != null) {
                Object studentId = data.get("studentId");
                log.info("Estudante criado - ID: {}", studentId);
                // Aqui podemos adicionar lógica para inicializar registros de notas, etc.
            }
        } catch (Exception e) {
            log.error("Erro ao processar evento student.created: {}", e.getMessage(), e);
        }
    }

    /**
     * Escuta eventos de estudantes atualizados
     */
    @KafkaListener(
        topics = "${microservice.kafka.topics.student-updated:distrischool.student.updated}",
        groupId = "${spring.application.name}-group"
    )
    public void consumeStudentUpdatedEvent(DistriSchoolEvent event) {
        log.info("Evento recebido - Student Updated: {}", event.getEventId());
        
        try {
            Map<String, Object> data = event.getData();
            if (data != null) {
                Object studentId = data.get("studentId");
                log.info("Estudante atualizado - ID: {}", studentId);
                // Aqui podemos adicionar lógica para sincronizar dados se necessário
            }
        } catch (Exception e) {
            log.error("Erro ao processar evento student.updated: {}", e.getMessage(), e);
        }
    }

    /**
     * Escuta eventos de estudantes deletados
     */
    @KafkaListener(
        topics = "${microservice.kafka.topics.student-deleted:distrischool.student.deleted}",
        groupId = "${spring.application.name}-group"
    )
    public void consumeStudentDeletedEvent(DistriSchoolEvent event) {
        log.info("Evento recebido - Student Deleted: {}", event.getEventId());
        
        try {
            Map<String, Object> data = event.getData();
            if (data != null) {
                Object studentId = data.get("studentId");
                log.info("Estudante deletado - ID: {}", studentId);
                // Aqui podemos adicionar lógica para limpar dados relacionados se necessário
            }
        } catch (Exception e) {
            log.error("Erro ao processar evento student.deleted: {}", e.getMessage(), e);
        }
    }

    /**
     * Escuta eventos de professores criados
     */
    @KafkaListener(
        topics = "${microservice.kafka.topics.teacher-created:distrischool.teacher.created}",
        groupId = "${spring.application.name}-group"
    )
    public void consumeTeacherCreatedEvent(DistriSchoolEvent event) {
        log.info("Evento recebido - Teacher Created: {}", event.getEventId());
        
        try {
            Map<String, Object> data = event.getData();
            if (data != null) {
                Object teacherId = data.get("teacherId");
                log.info("Professor criado - ID: {}", teacherId);
            }
        } catch (Exception e) {
            log.error("Erro ao processar evento teacher.created: {}", e.getMessage(), e);
        }
    }
}

