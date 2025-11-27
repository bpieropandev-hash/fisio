package com.physio.infrastructure.in.web.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeParseException;

@Configuration
public class JacksonConfig {

    // Registra um desserializador que aceita tanto ISO_LOCAL_DATE_TIME quanto timestamps ISO com 'Z'
    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        JavaTimeModule javaTimeModule = new JavaTimeModule();

        javaTimeModule.addDeserializer(LocalDateTime.class, new LenientLocalDateTimeDeserializer());

        // Também mantém o módulo padrão caso já use serialização de datas
        builder.modulesToInstall(javaTimeModule);

        // Opcional: garantir que datas sejam serializadas como strings ISO (não como timestamps numéricos)
        builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return builder;
    }

    // Desserializador simples e tolerante para LocalDateTime
    static class LenientLocalDateTimeDeserializer extends StdDeserializer<LocalDateTime> {
        protected LenientLocalDateTimeDeserializer() {
            super(LocalDateTime.class);
        }

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText().trim();
            if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
                return null;
            }
            try {
                // Caso contenha 'Z' ou offset, parse como Instant/OffsetDateTime e converte para LocalDateTime
                if (text.endsWith("Z") || text.matches(".*[+-]\\d{2}:?\\d{2}$")) {
                    try {
                        Instant inst = Instant.parse(text);
                        return LocalDateTime.ofInstant(inst, ZoneId.systemDefault());
                    } catch (DateTimeParseException e) {
                        // fallback para OffsetDateTime.parse se Instant.parse falhar com offset sem 'Z'
                        OffsetDateTime odt = OffsetDateTime.parse(text);
                        return odt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
                    }
                }
                // Caso sem offset, parse padrão
                return LocalDateTime.parse(text);
            } catch (Exception ex) {
                throw new JsonParseException(p, "Failed to deserialize java.time.LocalDateTime from value: " + text, ex);
            }
        }
    }
}
