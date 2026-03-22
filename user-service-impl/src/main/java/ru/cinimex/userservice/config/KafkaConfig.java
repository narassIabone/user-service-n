package ru.cinimex.userservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import ru.cinimex.userservice.dto.NotificationDto;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.kafka.config.TopicBuilder;

import java.util.Map;

@Configuration
public class KafkaConfig {


    @Value("${app.kafka.topic.registration:notification.message.in}")
    private String registrationTopicName;

    @Bean
    public NewTopic registrationTopic() {
        return TopicBuilder.name(registrationTopicName)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public ProducerFactory<String, NotificationDto> producerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, NotificationDto> kafkaTemplate(ProducerFactory<String, NotificationDto> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}