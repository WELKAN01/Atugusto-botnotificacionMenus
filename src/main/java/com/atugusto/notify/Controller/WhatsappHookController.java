package com.atugusto.notify.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.atugusto.notify.Service.MessageService;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/webhook")
public class WhatsappHookController {
    private final MessageService messageService;
    private ObjectMapper objectmapper;


    public WhatsappHookController(MessageService messageService) {
        this.messageService = messageService;
        this.objectmapper = new ObjectMapper();
    }



    @PostMapping
    // Ahora puedes acceder a los campos de manera tipada
    public ResponseEntity<String> receiveWebhook(@RequestBody WebhookWhatsapp payload) {
        System.out.println("Received payload: " + payload);
        String result=null;
        Entry entry = (payload.entry != null && !payload.entry.isEmpty()) ? payload.entry.get(0) : null;
        Change changes = (entry != null && !entry.changes.isEmpty()) ? entry.changes.get(0) : null;
        Value val = (changes != null) ? changes.value : null;
        String phoneNumberId = (val != null && val.metadata != null) ? val.metadata.phone_number_id : null;
        Message message = (val != null && val.messages != null && !val.messages.isEmpty()) ? val.messages.get(0) : null;
        String from = (message != null) ? message.from : null;
        String messageText = (message != null && message.text != null) ? message.text.body : null;
        
        if(val != null && val.statuses != null && !val.statuses.isEmpty()) {
            System.out.println("Status: " + val.statuses.get(0).status);
        } else {
            System.out.println("No statuses available");
        }

        if (message != null 
            && messageText != null) {
            System.out.println("messageText: " + messageText + " | phoneNumberId: " + phoneNumberId + " | from: " + (message.from != null ? message.from : "Unknown") +
                                 " | timestamp: " + message.timestamp + " | status: " + (val != null ? val.statuses : "No statuses" + "| from :"+ (message.from != null ? message.from : "Unknown")));
            result= messageService.sendMessage(new com.atugusto.notify.DTO.messageTO(phoneNumberId,from ,messageText));
        }
        
        
        if(message != null && message.interactive != null) {
            try{
                String jsonBonito = objectmapper.writerWithDefaultPrettyPrinter()
                                                .writeValueAsString(payload);
                System.out.println("=== PAYLOAD COMPLETO ===");
                System.out.println(jsonBonito);
                System.out.println("========================");
            }catch(Exception e){
                System.out.println("Error processing interactive message: " + e.getMessage());
            }
        }


        return ResponseEntity.ok("EVENT_RECEIVED");
    }

    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        String VERIFY_TOKEN = "mi_token_seguro";

        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            return ResponseEntity.ok(challenge);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error");
    }
}
