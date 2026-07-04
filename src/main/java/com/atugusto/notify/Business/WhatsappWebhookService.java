package com.atugusto.notify.Business;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.atugusto.notify.DTO.WebhookWhatsapp;
import com.atugusto.notify.DTO.WebhookWhatsapp.Change;
import com.atugusto.notify.DTO.WebhookWhatsapp.Entry;
import com.atugusto.notify.DTO.WebhookWhatsapp.Message;
import com.atugusto.notify.DTO.WebhookWhatsapp.Value;
import com.atugusto.notify.DTO.messageTO;
import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.Message.MensajeConfirmacion;
import com.atugusto.notify.Service.MessageService;
import com.atugusto.notify.Service.PlatosDiariosService;
import com.atugusto.notify.Service.PlatosService;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;

@Service
public class WhatsappWebhookService {
    private static final Logger logger = LoggerFactory.getLogger(WhatsappWebhookService.class);
    private static final String EVENT_RECEIVED_RESPONSE = "EVENT_RECEIVED";

    private final Map<String, List<Platos>> menuMemory = new ConcurrentHashMap<>();
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final PlatosService platosService;
    private final PlatosDiariosService platosDiariosService;

    public WhatsappWebhookService(
            MessageService messageService,
            ObjectMapper objectMapper,
            PlatosService platosService,
            PlatosDiariosService platoDiarioService) {
        this.messageService = messageService;
        this.objectMapper = objectMapper;
        this.platosService = platosService;
        this.platosDiariosService = platoDiarioService;
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
            //if(message.interactive.list_reply.id)
            handleConfirmationAction(message.interactive.list_reply.id,value,message);
            processInteractiveMessage(payload, value, message);
            
            return Mono.just(EVENT_RECEIVED_RESPONSE);
        }

        if ("text".equals(message.type)) {
            logger.info("mensaje de bienvenida(muestra de platos)");
            processTextMessage(value, message);
            sendConfirmationIfOrderExists(value, message);
        }

        return Mono.just(EVENT_RECEIVED_RESPONSE);
    }

    private void handleConfirmationAction(String action, Value value, Message message) {
        if(action==null){
            return ;
        }
        switch (action) {
            case MensajeConfirmacion.AGREGAR:
                handleAgregar(value, message);
                break;
            case MensajeConfirmacion.CONFIRMAR:
                handleConfirmar(value, message);
                break;
            
            case MensajeConfirmacion.CANCELAR:
                handleCancelar(message);
                break;

            default:
                logger.warn("No es un mensaje de confirmacion: {}",action);
        }
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

    private void logWebhookStatus(Value value) {
        if (value != null && value.statuses != null && !value.statuses.isEmpty()) {
            logger.info("WhatsApp status received: {}", value.statuses.get(0).status);
            return;
        }

        logger.debug("WhatsApp webhook without statuses");
    }

    private void processInteractiveMessage(WebhookWhatsapp payload, Value value, Message message) {
        logInteractivePayload(payload, message);
        sendConfirmationIfOrderExists(value, message);
    }

    private void processTextMessage(Value value, Message message) {
        if (value == null || message == null || message.text == null) {
            return;
        }

        String phoneNumberId = value.metadata != null ? value.metadata.phone_number_id : null;
        String from = message.from;
        String messageText = message.text.body;

        if (!StringUtils.hasText(phoneNumberId) || !StringUtils.hasText(from) || !StringUtils.hasText(messageText)) {
            logger.warn("Incomplete WhatsApp text message. phoneNumberId={}, from={}, timestamp={}",
                    phoneNumberId, from, message.timestamp);
            return;
        }

        logger.info("WhatsApp text message received. from={}, phoneNumberId={}, timestamp={}",
                from, phoneNumberId, message.timestamp);

        messageService.sendMessage(new messageTO(phoneNumberId, from, messageText))
                .subscribe(
                        response -> logger.info("WhatsApp response sent to {}. Meta response={}", from, response),
                        exception -> logger.error("Error sending WhatsApp response to {}", from, exception));
    }

    private void logInteractivePayload(WebhookWhatsapp payload, Message message) {
        if (message == null || message.interactive == null) {
            return;
        }

        try {
            String selectedMenu = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            logger.info("Interactive WhatsApp payload received:\n{}", selectedMenu);
            Platos plato = platosService.findIDPlatos(Long.valueOf(message.interactive.list_reply.id));
            logger.info("menu seleccionado : {} ,descripcion {} , precio {}",
                    message.interactive.list_reply.title,
                    message.interactive.list_reply.description,
                    message.interactive.list_reply.precio);
            saveMemoryMenu(message.from, plato);
        } catch (Exception exception) {
            logger.error("Error logging interactive WhatsApp payload", exception);
        }
    }

    private void saveMemoryMenu(String from, Platos plato) {
        List<Platos> platos = menuMemory.computeIfAbsent(from, key -> new ArrayList<>());

        boolean yaExiste = platos.stream()
                .anyMatch(item -> item.getId().equals(plato.getId()));

        if (!yaExiste) {
            platos.add(plato);
            logger.info("Plato agregado. Lista de pedido: {}", platos);
        } else {
            logger.info("Plato ya registrado, ignorando duplicado: {}", plato.getNombre());
        }
    }

    private void sendConfirmationIfOrderExists(Value value, Message message) {
        if (value == null || value.metadata == null || message == null || !menuMemory.containsKey(message.from)) {
            return;
        }

        messageService.sendMessageConfirm(
                new messageTO(value.metadata.phone_number_id, message.from, null),
                menuMemory)
                .subscribe(
                        response -> logger.info("Confirm enviado a {}", message.from),
                        error -> logger.error("Error enviando confirm a {}", message.from, error));
    }


    private void handleConfirmar(Value value, Message message) {
        logger.info("Pedido confirmado para {}", message.from);
        // siguiente paso del flujo
        // por ejemplo: enviar resumen final o pedir direccion/pago
    }

    private void handleAgregar(Value value, Message message) {
        logger.info("Usuario quiere agregar otro plato: {}", message.from);

        messageService.sendMessage(
            new messageTO(value.metadata.phone_number_id, message.from, "menu")
        ).subscribe(
            response -> logger.info("Menu reenviado a {}", message.from),
            error -> logger.error("Error reenviando menu a {}", message.from, error)
        );
    }

    private void handleCancelar(Message message) {
        logger.info("Cancelando pedido de {}", message.from);
        menuMemory.remove(message.from);
    }

    
}
