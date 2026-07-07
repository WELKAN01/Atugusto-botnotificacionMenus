package com.atugusto.notify.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.atugusto.notify.Business.WhatsappWebhookService;
import com.atugusto.notify.DTO.WebhookWhatsapp;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/webhook")
public class WhatsappHookController {
    private final WhatsappWebhookService whatsappWebhookService;
    private final String verifyToken;

    public WhatsappHookController(
            WhatsappWebhookService whatsappWebhookService,
            @Value("${meta.whatsapp.verify-token:mi_token_seguro}") String verifyToken) {
        this.whatsappWebhookService = whatsappWebhookService;
        this.verifyToken = verifyToken;
    }

    @PostMapping
    public Mono<ResponseEntity<String>> receiveWebhook(@RequestBody WebhookWhatsapp payload) {
        return whatsappWebhookService.processWebhook(payload)
                .map(ResponseEntity::ok);
    }

    @GetMapping
    public Mono<ResponseEntity<String>> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return Mono.just(ResponseEntity.ok(challenge));
        }

        return Mono.just(ResponseEntity.status(403).body("Error"));
    }
}
