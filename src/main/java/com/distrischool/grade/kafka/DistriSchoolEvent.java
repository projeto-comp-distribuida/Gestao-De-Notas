package com.distrischool.grade.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistriSchoolEvent {
    private String eventId;
    private String eventType;
    private String source;
    private LocalDateTime timestamp;
    private Map<String, Object> data;
    private Map<String, Object> metadata;

    public static DistriSchoolEvent of(String eventType, String source, Map<String, Object> data) {
        return DistriSchoolEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .source(source)
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }
}

