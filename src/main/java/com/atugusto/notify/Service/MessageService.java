package com.atugusto.notify.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.atugusto.notify.DTO.messageTO;
import com.atugusto.notify.Entity.Platos;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class MessageService {
    private final WebClient webclient;
    private final PlatosService platosService;
    private final String metaWhatsappToken;

    public MessageService(
            WebClient webclient,
            PlatosService platosService,
            @Value("${meta.whatsapp.token:}") String metaWhatsappToken) {
        this.webclient = webclient;
        this.platosService = platosService;
        this.metaWhatsappToken = metaWhatsappToken;
    }

    public Mono<String> sendMessage(messageTO message) {
        if (metaWhatsappToken == null || metaWhatsappToken.isBlank()) {
            return Mono.error(new IllegalStateException("META_WHATSAPP_TOKEN is not configured"));
        }

        return Mono.fromCallable(() -> sendMessageTemplateWithPlatos(message))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(body -> webclient.post()
                        .uri(String.format("https://graph.facebook.com/v25.0/%s/messages", message.getPhone_number_id()))
                        .headers(headers -> {
                            headers.setBearerAuth(metaWhatsappToken);
                            headers.setContentType(MediaType.APPLICATION_JSON);
                        })
                        .bodyValue(body)
                        .retrieve()
                        .onStatus(status -> status.isError(),
                                response -> response.bodyToMono(String.class)
                                        .map(bodyResponse -> new RuntimeException("Error: " + bodyResponse)))
                        .bodyToMono(String.class));
    }


    private Map<String, Object> sendMessageTemplate(messageTO message) {

    Map<String, Object> body = Map.of(
        "messaging_product", "whatsapp",
        "to", message.getPhoneNumber(),
        "type", "interactive",
        "interactive", Map.of(
            "type", "list",
            "body", Map.of(
                "text", "🍽️ Menú de hoy"
            ),
            "action", Map.of(
                "button", "Ver platos",
                "sections", List.of(
                    Map.of(
                        "title", "Platos",
                        "rows", List.of(
                            Map.of(
                                "id", "lomo",
                                "title", "Lomo Saltado",
                                "description", "S/ 32"
                            )
                        )
                    )
                )
            )
        )
    );
    return body;
    }

    private Map<String, Object> sendMessageTemplateWithPlatos(messageTO message) {
        // Obtener platos disponibles desde la base de datos
        List<Platos> platosDisponibles = platosService.findPlatosDisponibles();
        
        // Construir dinámicamente la lista de platos
        List<Map<String, String>> rows = platosDisponibles.stream()
                .map(plato -> Map.of(
                        "id", String.valueOf(plato.getId()),
                        "title", plato.getNombre(),
                        //"precio", String.valueOf(plato.getPrecio()),
                        "description", String.format("%s - S/ %.2f", plato.getDescripcion(), plato.getPrecio())
                ))
                .collect(Collectors.toList());

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", message.getPhoneNumber(),
                "type", "interactive",
                "interactive", Map.of(
                        "type", "list",
                        "body", Map.of(
                                "text", "🍽️ Menú de hoy"
                        ),
                        "action", Map.of(
                                "button", "Ver platos",
                                "sections", List.of(
                                        Map.of(
                                                "title", "Platos disponibles",
                                                "rows", rows
                                        )
                                )
                        )
                )
        );
        return body;
    }
}
