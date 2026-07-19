package com.atugusto.notify.Service;

import com.atugusto.notify.DTO.messageTO;
import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.Message.MensajeConfirmacion;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    private final WebClient webclient;
    private final String metaWhatsappToken;
    private final PlatosDiariosService platosDiariosService;

    public MessageService(
            WebClient webclient,
            @Value("${meta.whatsapp.token:}") String metaWhatsappToken,
            PlatosDiariosService platosDiariosService) {
        this.webclient = webclient;
        this.metaWhatsappToken = metaWhatsappToken;
        this.platosDiariosService = platosDiariosService;
    }

    public Mono<String> sendMessage(messageTO message) {
        if (metaWhatsappToken == null || metaWhatsappToken.isBlank()) {
            return Mono.error(new IllegalStateException("META_WHATSAPP_TOKEN is not configured"));
        }

        return sendMessageTemplateWithPlatos(message)
                .flatMap(body -> sendToMeta(message.getPhone_number_id(), body));
    }

    public Mono<String> sendMessageConfirm(messageTO message, Map<String, List<Platos>> memory) {
        if (metaWhatsappToken == null || metaWhatsappToken.isBlank()) {
            return Mono.error(new IllegalStateException("META_WHATSAPP_TOKEN is not configured"));
        }

        return Mono.fromSupplier(() -> sendMessageTemplateConfirmed(message, memory))
                .flatMap(body -> sendToMeta(message.getPhone_number_id(), body));
    }

    public Mono<String> sendOrderSaved(messageTO message, List<Platos> platosSeleccionados) {
        if (metaWhatsappToken == null || metaWhatsappToken.isBlank()) {
            return Mono.error(new IllegalStateException("META_WHATSAPP_TOKEN is not configured"));
        }

        return Mono.fromSupplier(() -> buildSavedOrderMessage(message, platosSeleccionados))
                .flatMap(body -> sendToMeta(message.getPhone_number_id(), body));
    }

    private Mono<String> sendToMeta(String phoneNumberId, Map<String, Object> body) {
        return webclient.post()
                .uri(String.format("https://graph.facebook.com/v25.0/%s/messages", phoneNumberId))
                .headers(headers -> {
                    headers.setBearerAuth(metaWhatsappToken);
                    headers.setContentType(MediaType.APPLICATION_JSON);
                })
                .bodyValue(body)
                .retrieve()
                .onStatus(status -> status.isError(),
                        response -> response.bodyToMono(String.class)
                                .map(bodyResponse -> new RuntimeException("Error: " + bodyResponse)))
                .bodyToMono(String.class);
    }

    private Mono<Map<String, Object>> sendMessageTemplateWithPlatos(messageTO message) {
        logger.info("Obteniendo platos disponibles para el dia de hoy.");
        return platosDiariosService.platosDiariosListToday()
                .collectList()
                .map(platosDisponibles -> {
                    logger.info("Platos disponibles: {}",
                            platosDisponibles.stream().map(Platos::getNombre).collect(Collectors.joining(", ")));

                    List<Map<String, String>> rows = platosDisponibles.stream()
                            .map(plato -> Map.of(
                                    "id", String.valueOf(plato.getId()),
                                    "title", plato.getNombre(),
                                    "description",
                                    String.format("%s - S/ %.2f", plato.getDescripcion(), plato.getPrecio())))
                            .toList();

                    return Map.<String, Object>of(
                            "messaging_product", "whatsapp",
                            "to", message.getPhoneNumber(),
                            "type", "interactive",
                            "interactive", Map.of(
                                    "type", "list",
                                    "body", Map.of("text", "Menu de hoy"),
                                    "action", Map.of(
                                            "button", "Ver platos",
                                            "sections", List.of(
                                                    Map.of(
                                                            "title", "Platos disponibles",
                                                            "rows", rows)))));
                });
    }

    private Map<String, Object> sendMessageTemplateConfirmed(messageTO message, Map<String, List<Platos>> menusChoosed) {
        String menulist = menusChoosed.getOrDefault(message.getPhoneNumber(), List.of()).stream()
                .map(plato -> String.format("%s - S/ %.2f", plato.getNombre(), plato.getPrecio()))
                .collect(Collectors.joining("\n- "));

        if (menulist.isBlank()) {
            menulist = "No hay platos seleccionados todavia.";
        } else {
            menulist = "- " + menulist;
        }

        return Map.of(
                "messaging_product", "whatsapp",
                "to", message.getPhoneNumber(),
                "type", "interactive",
                "interactive", Map.of(
                        "type", "list",
                        "body", Map.of("text", "Menu escogido:\n" + menulist),
                        "action", Map.of(
                                "button", "Que realizar?",
                                "sections", List.of(
                                        Map.of(
                                                "title", "Pedido",
                                                "rows", MensajeConfirmacion.obtenerLista())))));
    }

    private Map<String, Object> buildSavedOrderMessage(messageTO message, List<Platos> platosSeleccionados) {
        String detallePedido = platosSeleccionados.stream()
                .map(plato -> String.format("- %s - S/ %.2f", plato.getNombre(), plato.getPrecio()))
                .collect(Collectors.joining("\n"));

        String texto = detallePedido.isBlank()
                ? "Tu pedido fue confirmado."
                : "Tu pedido fue confirmado con estos platos:\n" + detallePedido;

        return Map.of(
                "messaging_product", "whatsapp",
                "to", message.getPhoneNumber(),
                "type", "text",
                "text", Map.of("body", texto));
    }
}
