package com.atugusto.notify.Business;

import com.atugusto.notify.DTO.WebhookWhatsapp;
import com.atugusto.notify.DTO.WebhookWhatsapp.Change;
import com.atugusto.notify.DTO.WebhookWhatsapp.Entry;
import com.atugusto.notify.DTO.WebhookWhatsapp.Message;
import com.atugusto.notify.DTO.WebhookWhatsapp.Value;
import com.atugusto.notify.DTO.messageTO;
import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.Message.MensajeConfirmacion;
import com.atugusto.notify.Service.MessageService;
import com.atugusto.notify.Service.PlatosService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

@Service
public class WhatsappWebhookService {
    private static final Logger logger = LoggerFactory.getLogger(WhatsappWebhookService.class);
    private static final String EVENT_RECEIVED_RESPONSE = "EVENT_RECEIVED";

    private final Map<String, List<Platos>> menuMemory = new ConcurrentHashMap<>();
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final PlatosService platosService;

    public WhatsappWebhookService(
            MessageService messageService,
            ObjectMapper objectMapper,
            PlatosService platosService) {
        this.messageService = messageService;
        this.objectMapper = objectMapper;
        this.platosService = platosService;
    }

    public Mono<String> processWebhook(WebhookWhatsapp payload) {
        logger.info("WhatsApp webhook received");

        Value value = getFirstValue(payload);
        Message message = getFirstMessage(value);

        logWebhookStatus(value);

        if (message == null) {
            return Mono.just(EVENT_RECEIVED_RESPONSE);
        }

        if ("interactive".equals(message.type)) {
            return processInteractiveMessage(payload, value, message)
                    .thenReturn(EVENT_RECEIVED_RESPONSE);
        }

        if ("text".equals(message.type)) {
            return processTextMessage(value, message)
                    .thenReturn(EVENT_RECEIVED_RESPONSE);
        }

        return Mono.just(EVENT_RECEIVED_RESPONSE);
    }

    private Mono<Void> processInteractiveMessage(WebhookWhatsapp payload, Value value, Message message) {
        String action = getInteractiveAction(message);

        if (MensajeConfirmacion.AGREGAR.equals(action)) {
            return handleAgregar(value, message);
        }

        if (MensajeConfirmacion.CONFIRMAR.equals(action)) {
            return handleConfirmar(value, message);
        }

        if (MensajeConfirmacion.CANCELAR.equals(action)) {
            return handleCancelar(message);
        }

        return logInteractivePayload(payload, message)
                .flatMap(plato -> saveMemoryMenu(message.from, plato))
                .then(Mono.defer(() -> sendConfirmationIfOrderExists(value, message)));
    }

    private Mono<Void> processTextMessage(Value value, Message message) {
        if (value == null || message == null || message.text == null) {
            return Mono.empty();
        }

        String phoneNumberId = value.metadata != null ? value.metadata.phone_number_id : null;
        String from = message.from;
        String messageText = message.text.body;

        if (!StringUtils.hasText(phoneNumberId) || !StringUtils.hasText(from) || !StringUtils.hasText(messageText)) {
            logger.warn("Incomplete WhatsApp text message. phoneNumberId={}, from={}, timestamp={}",
                    phoneNumberId, from, message.timestamp);
            return Mono.empty();
        }

        logger.info("WhatsApp text message received. from={}, phoneNumberId={}, timestamp={}",
                from, phoneNumberId, message.timestamp);

        if (!isMenuRequest(messageText)) {
            logger.info("Text message does not request menu. from={}, text={}", from, messageText);
            return Mono.empty();
        }

        logger.info("Menu requested by {}", from);
        return messageService.sendMessage(new messageTO(phoneNumberId, from, messageText))
                .doOnNext(response -> logger.info("WhatsApp response sent to {}. Meta response={}", from, response))
                .doOnError(exception -> logger.error("Error sending WhatsApp response to {}", from, exception))
                .then();
    }

    private Value getFirstValue(WebhookWhatsapp payload) {
        if (payload == null) {
            return null;
        }

        Entry entry = payload.entry != null && !payload.entry.isEmpty()
                ? payload.entry.get(0)
                : null;

        Change change = entry != null && entry.changes != null && !entry.changes.isEmpty()
                ? entry.changes.get(0)
                : null;

        return change != null ? change.value : null;
    }

    private Message getFirstMessage(Value value) {
        return value != null && value.messages != null && !value.messages.isEmpty()
                ? value.messages.get(0)
                : null;
    }

    private String getInteractiveAction(Message message) {
        if (message == null || message.interactive == null) {
            return null;
        }

        if (message.interactive.list_reply != null) {
            return message.interactive.list_reply.id;
        }

        if (message.interactive.button_reply != null) {
            return message.interactive.button_reply.id;
        }

        return null;
    }

    private void logWebhookStatus(Value value) {
        if (value != null && value.statuses != null && !value.statuses.isEmpty()) {
            logger.info("WhatsApp status received: {}", value.statuses.get(0).status);
            return;
        }

        logger.debug("WhatsApp webhook without statuses");
    }

    private boolean isMenuRequest(String messageText) {
        String normalizedText = Normalizer.normalize(messageText, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();

        return normalizedText.equals("menu")
                || normalizedText.equals("hola")
                || normalizedText.equals("inicio")
                || normalizedText.equals("empezar")
                || normalizedText.equals("ver menu")
                || normalizedText.equals("quiero menu")
                || normalizedText.equals("quiero ver el menu");
    }

    private Mono<Platos> logInteractivePayload(WebhookWhatsapp payload, Message message) {
        if (message == null || message.interactive == null || message.interactive.list_reply == null) {
            return Mono.empty();
        }

        try {
            String selectedMenu = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            logger.info("Interactive WhatsApp payload received:\n{}", selectedMenu);
            logger.info("menu seleccionado : {} ,descripcion {} , precio {}",
                    message.interactive.list_reply.title,
                    message.interactive.list_reply.description,
                    message.interactive.list_reply.precio);
        } catch (Exception exception) {
            logger.error("Error logging interactive WhatsApp payload", exception);
        }

        return parsePlatoId(message.interactive.list_reply.id)
                .flatMap(platosService::findIDPlatos)
                .doOnNext(plato -> logger.info("Plato seleccionado cargado desde BD: {}", plato.getNombre()))
                .switchIfEmpty(Mono.defer(() -> {
                    logger.warn("No se encontro plato para id {}", message.interactive.list_reply.id);
                    return Mono.empty();
                }));
    }

    private Mono<Long> parsePlatoId(String platoId) {
        try {
            return Mono.just(Long.valueOf(platoId));
        } catch (NumberFormatException exception) {
            logger.warn("El id recibido no corresponde a un plato persistido: {}", platoId);
            return Mono.empty();
        }
    }

    private Mono<Void> saveMemoryMenu(String from, Platos plato) {
        if (plato == null) {
            return Mono.empty();
        }

        return Mono.fromRunnable(() -> {
            List<Platos> platos = menuMemory.computeIfAbsent(from, key -> new ArrayList<>());
            boolean yaExiste = platos.stream().anyMatch(item -> item.getId().equals(plato.getId()));

            if (!yaExiste) {
                platos.add(plato);
                logger.info("Plato agregado. Lista de pedido: {}", platos);
            } else {
                logger.info("Plato ya registrado, ignorando duplicado: {}", plato.getNombre());
            }
        });
    }

    private Mono<Void> sendConfirmationIfOrderExists(Value value, Message message) {
        if (value == null || value.metadata == null || message == null || !menuMemory.containsKey(message.from)) {
            logger.info("No hay pedido registrado para {}, no se enviará confirmación", message.from);
            return Mono.empty();
        }

        return messageService.sendMessageConfirm(
                        new messageTO(value.metadata.phone_number_id, message.from, null),
                        menuMemory)
                .doOnNext(response -> logger.info("Confirm enviado a {}", message.from))
                .doOnError(error -> logger.error("Error enviando confirm a {}", message.from, error))
                .then();
    }

    private Mono<Void> handleConfirmar(Value value, Message message) {
        if (value == null || value.metadata == null || message == null) {
            return Mono.empty();
        }

        List<Platos> platosSeleccionados = menuMemory.get(message.from);
        if (platosSeleccionados == null || platosSeleccionados.isEmpty()) {
            logger.info("No hay platos para confirmar para {}", message.from);
            return Mono.empty();
        }

        logger.info("Pedido confirmado para {}", message.from);
        return messageService.sendOrderSaved(
                        new messageTO(value.metadata.phone_number_id, message.from, null),
                        platosSeleccionados)
                .doOnNext(response -> logger.info("Confirmacion final enviada a {}", message.from))
                .doOnError(error -> logger.error("Error enviando confirmacion final a {}", message.from, error))
                .doOnSuccess(ignored -> menuMemory.remove(message.from))
                .then();
    }

    private Mono<Void> handleAgregar(Value value, Message message) {
        logger.info("Usuario quiere agregar otro plato: {}", message.from);
        return messageService.sendMessage(new messageTO(value.metadata.phone_number_id, message.from, "menu"))
                .doOnNext(response -> logger.info("Menu reenviado a {}", message.from))
                .doOnError(error -> logger.error("Error reenviando menu a {}", message.from, error))
                .then();
    }

    private Mono<Void> handleCancelar(Message message) {
        logger.info("Cancelando pedido de {}", message.from);
        menuMemory.remove(message.from);
        return Mono.empty();
    }
}
