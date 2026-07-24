package com.atugusto.notify.Service;

import com.atugusto.notify.Entity.Platos;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Service
public class MenuMemoryService {
    private static final Logger logger = LoggerFactory.getLogger(MenuMemoryService.class);
    private static final TypeReference<List<Platos>> PLATOS_LIST_TYPE = new TypeReference<>() {};
    private static final String KEY_PREFIX = "whatsapp:menu-memory:";

    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public MenuMemoryService(
            ReactiveStringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${app.menu-memory.ttl-minutes:30}") long ttlMinutes) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofMinutes(ttlMinutes);
    }

    public Mono<List<Platos>> getMenu(String from) {
        if (!StringUtils.hasText(from)) {
            return Mono.just(List.of());
        }

        return redisTemplate.opsForValue()
                .get(buildKey(from))
                .flatMap(this::deserializePlatos)
                .defaultIfEmpty(List.of());
    }

    public Mono<List<Platos>> addPlato(String from, Platos plato) {
        if (!StringUtils.hasText(from) || plato == null) {
            return Mono.just(List.of());
        }

        return getMenu(from)
                .map(platosActuales -> addIfAbsent(platosActuales, plato))
                .flatMap(platosActualizados -> saveMenu(from, platosActualizados).thenReturn(platosActualizados));
    }

    public Mono<Void> removeMenu(String from) {
        if (!StringUtils.hasText(from)) {
            return Mono.empty();
        }

        return redisTemplate.delete(buildKey(from)).then();
    }

    private List<Platos> addIfAbsent(List<Platos> platosActuales, Platos plato) {
        List<Platos> actualizados = new ArrayList<>(platosActuales);
        boolean yaExiste = actualizados.stream().anyMatch(item -> item.getId().equals(plato.getId()));

        if (!yaExiste) {
            actualizados.add(plato);
        }

        return actualizados;
    }

    private Mono<Boolean> saveMenu(String from, List<Platos> platos) {
        return serializePlatos(platos)
                .flatMap(payload -> redisTemplate.opsForValue().set(buildKey(from), payload, ttl));
    }

    private Mono<String> serializePlatos(List<Platos> platos) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(platos))
                .onErrorMap(JsonProcessingException.class,
                        exception -> new IllegalStateException("No se pudo serializar el menu temporal", exception));
    }

    private Mono<List<Platos>> deserializePlatos(String payload) {
        return Mono.fromCallable(() -> objectMapper.readValue(payload, PLATOS_LIST_TYPE))
                .onErrorResume(exception -> {
                    logger.error("No se pudo deserializar el menu temporal almacenado en Redis", exception);
                    return Mono.just(List.of());
                });
    }

    private String buildKey(String from) {
        return KEY_PREFIX + from;
    }
}
