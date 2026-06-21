package com.atugusto.notify.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.atugusto.notify.DTO.messageTO;
import com.atugusto.notify.Entity.Platos;

@Service
public class MessageService {
    private final WebClient webclient;
    private final PlatosService platosService;

    public MessageService(WebClient webclient, PlatosService platosService) {
        this.webclient = webclient;
        this.platosService = platosService;
    }

    public String sendMessage(messageTO message) {

        return webclient.post()
                .uri(String.format("https://graph.facebook.com/v25.0/%s/messages", message.getPhone_number_id()))
                .headers(headers -> {
                    headers.setBearerAuth("EAAUy52IkCTEBRWPYuN4stFcOIEvXgulL4qCZCOSntYGZC2Ref34OQIo3nqZAlyPg30OF3YNEhsWmpTs5y3nde3DX8MiO4QJqp0at0D0iU6LsZAgHAS0gbbP8cQZBGcb1UxaksZAIRG7gg4F0zGGrsSoZChS6ZBgNtoXwoYa0hWZB7awfog1rSgzPK64BJTeBBgJTAMFXglzUQBntrBrxZAsXjtJRP9jpkE5bkFqRfEDw1de71fLtCXjhze6Q4wj68ZCQPy4UL15Jleb87aUnVLuaCIbN5i9tgZDZD");
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
