package com.atugusto.notify.Controller;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atugusto.notify.DTO.WebhookWhatsapp;
import com.atugusto.notify.DTO.WebhookWhatsapp.Change;
import com.atugusto.notify.DTO.WebhookWhatsapp.Entry;
import com.atugusto.notify.DTO.WebhookWhatsapp.Message;
import com.atugusto.notify.DTO.WebhookWhatsapp.Value;
import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.DTO.messageTO;
import com.atugusto.notify.Service.MessageService;
import com.atugusto.notify.Service.PlatosService;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/webhook")
public class WhatsappHookController {
    private static final Logger logger = LoggerFactory.getLogger(WhatsappHookController.class);
    private static final String VERIFY_TOKEN = "mi_token_seguro";
    private static final String EVENT_RECEIVED_RESPONSE = "EVENT_RECEIVED";
    
    private final HashMap<String,List<Platos>> menumemory;
    private final PlatosService platosService;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;

    public WhatsappHookController(MessageService messageService, ObjectMapper objectMapper,HashMap<String,List<Platos>> menumemory,PlatosService platosService) {
        this.messageService = messageService;
        this.objectMapper = objectMapper;
        this.menumemory = menumemory;
        this.platosService = platosService;
    }

    @PostMapping
    public ResponseEntity<String> receiveWebhook(@RequestBody WebhookWhatsapp payload) {
        logger.info("WhatsApp webhook received");
        
        Value value = getFirstValue(payload);
        Message message = getFirstMessage(value);

        logWebhookStatus(value); // log de estado de webhook en meta
        processTextMessage(value, message); //
        logInteractivePayload(payload, message);

        return ResponseEntity.ok(EVENT_RECEIVED_RESPONSE);
    }

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            return ResponseEntity.ok(challenge);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error");
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
            String SelectedMenu = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
            logger.info("Interactive WhatsApp payload received:\n{}", SelectedMenu);
            Platos plato=platosService.findIDPlatos(Long.valueOf(message.interactive.list_reply.id));
            logger.info("menu seleccionado : {} ,descripcion {} , precio {}",
                                        message.interactive.list_reply.title,
                                        message.interactive.list_reply.description,
                                        message.interactive.list_reply.precio);
            saveMemoryMenu(message.from,plato);
        } catch (Exception exception) {
            logger.error("Error logging interactive WhatsApp payload", exception);
        }
    }

    private void saveMemoryMenu(String from,Platos plato) {
        if(menumemory.containsKey(from)){
            List<Platos> platos = new ArrayList<>();
            platos.add(plato);

            menumemory.put(from, platos);
        }else{
            menumemory.get(from).add(plato);
            logger.info("se agrego un nuevo plato. lista de pedido : {}",menumemory.get(from).toString());
        }
        return ;
    }
}
