package com.distrischool.grade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Aplicação principal do microserviço de gestão de notas.
 * 
 * @EnableFeignClients - Habilita comunicação com outros microserviços via Feign
 * @EnableKafka - Habilita integração com Apache Kafka
 * @EnableCaching - Habilita cache do Spring
 */
@SpringBootApplication
@EnableFeignClients
@EnableKafka
@EnableCaching
public class GradeManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(GradeManagementApplication.class, args);
    }
}

