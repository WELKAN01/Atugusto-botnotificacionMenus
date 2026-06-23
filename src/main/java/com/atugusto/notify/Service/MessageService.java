package com.atugusto.notify.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.atugusto.notify.DTO.messageTO;
import com.atugusto.notify.Entity.Platos;

@Service
public class MessageService {
    private final WebClient webclient;
    private final PlatosService platosService;
    private final String metaWhatsappToken;

    public MessageService(
            WebClient webclient,
            PlatosService platosService,
            @Value("${META_WHATSAPP_TOKEN:}") String metaWhatsappToken) {
        this.webclient = webclient;
        this.platosService = platosService;
        this.metaWhatsappToken = metaWhatsappToken;
    }

    public String sendMessage(messageTO message) {
        if (metaWhatsappToken == null || metaWhatsappToken.isBlank()) {
            throw new IllegalStateException("META_WHATSAPP_TOKEN is not configured");
        }

        return webclient.post()
                .uri(String.format("https://graph.facebook.com/v25.0/%s/messages", message.getPhone_number_id()))
                .headers(headers -> {
                    headers.setBearerAuth(metaWhatsappToken);
                    headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                        }
                )
                .bodyValue(sendMessageTemplateWithPlatos(message))
                .retrieve()
                .onStatus(status -> status.isError(),
                        response -> response.bodyToMono(String.class).map(body -> new RuntimeException("Error: " + body)))
                .bodyToMono(String.class)
                .block();
        
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
