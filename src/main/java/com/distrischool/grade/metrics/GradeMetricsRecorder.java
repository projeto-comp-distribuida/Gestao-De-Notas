package com.distrischool.grade.metrics;

import com.distrischool.grade.entity.Grade.GradeStatus;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/**
 * Registra métricas de domínio para o microserviço de notas.
 */
@Component
public class GradeMetricsRecorder {

    private static final String METRIC_GRADE_OPERATIONS = "grade_operations_total";
    private static final String METRIC_GRADE_STATUS_UPDATES = "grade_status_changes_total";
    private static final String METRIC_GRADE_EVENTS = "grade_events_total";

    private final MeterRegistry meterRegistry;

    public GradeMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordOperation(String operation, String outcome) {
        meterRegistry.counter(
            METRIC_GRADE_OPERATIONS,
            "operation", operation,
            "outcome", outcome
        ).increment();
    }

    public void recordStatusChange(GradeStatus status) {
        if (status == null) {
            return;
        }
        meterRegistry.counter(
            METRIC_GRADE_STATUS_UPDATES,
            "status", status.name().toLowerCase()
        ).increment();
    }

    public void recordKafkaEvent(String direction, String eventType, String outcome) {
        meterRegistry.counter(
            METRIC_GRADE_EVENTS,
            "direction", direction,
            "event_type", eventType,
            "outcome", outcome
        ).increment();
    }
}


