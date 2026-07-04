package com.atugusto.notify.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.atugusto.notify.DTO.messageTO;
import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.Message.MensajeConfirmacion;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class MessageService {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);
    private final WebClient webclient;
    private final PlatosService platosService;
    private final String metaWhatsappToken;
    private final PlatosDiariosService platosDiariosService;

    public MessageService(
            WebClient webclient,
            PlatosService platosService,
            @Value("${meta.whatsapp.token:}") String metaWhatsappToken,
            PlatosDiariosService platosDiariosService) {
        this.webclient = webclient;
        this.platosService = platosService;
        this.metaWhatsappToken = metaWhatsappToken;
        this.platosDiariosService = platosDiariosService;
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

    public Mono<String> sendMessageConfirm(messageTO message, Map<String, List<Platos>> memory){
        if (metaWhatsappToken == null || metaWhatsappToken.isBlank()) {
            return Mono.error(new IllegalStateException("META_WHATSAPP_TOKEN is not configured"));
        }

        return Mono.fromCallable(() -> sendMessageTemplateConfirmed(message,memory))
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
        logger.info("Obteniendo platos disponibles para el día de hoy.");
        List<Platos> platosDisponibles = platosDiariosService.PlatosDiariosListToday();
        //List<Platos> platosDisponibles = platosService.findPlatosDisponibles();
        // Construir dinámicamente la lista de platos
        logger.info("Platos disponibles: {}", platosDisponibles.stream().map(Platos::getNombre).collect(Collectors.joining(", ")));
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


    private Map<String,Object> sendMessageTemplateConfirmed(messageTO message, Map<String, List<Platos>> menuschoosed){
        String menulist = menuschoosed.get(message.getPhoneNumber()).stream().map(Platos::getDescripcion).collect(Collectors.joining("\n• ", "• ", ""));
        
        

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", message.getPhoneNumber(),
                "type", "interactive",
                "interactive", Map.of(
                        "type", "list",
                        "body", Map.of(
                                "text", "🍽️ Menú escogido: "+menulist
                        ),
                        "action", Map.of(
                                "button", "¿que realizar?",
                                "sections", List.of(
                                        Map.of(
                                        "title", "Pedido",
                                        "rows", MensajeConfirmacion.obtenerLista()
                                )
                                )
                        )
                )
        );
        return body;
    }
}
