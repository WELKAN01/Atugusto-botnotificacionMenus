package com.atugusto.notify.Controller;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private static final String VERIFY_TOKEN = "mi_token_seguro";

    private final WhatsappWebhookService whatsappWebhookService;

    public WhatsappHookController(WhatsappWebhookService whatsappWebhookService) {
        this.whatsappWebhookService = whatsappWebhookService;
    }

    @PostMapping
    public Mono<ResponseEntity<String>> receiveWebhook(@RequestBody WebhookWhatsapp payload) {
        return Mono.just(payload)
                .flatMap(whatsappWebhookService::processWebhook)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        new ConcurrentHashMap<String, String>() {{
                        put("statusCode", HttpStatus.BAD_REQUEST.toString());
                        put("error", "Invalid payload");
                        put("payload", payload.toString());
                    }}
                .toString()));
    }

    @GetMapping
    public ResponseEntity<Mono<String>> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {

        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            return ResponseEntity.ok(Mono.just(challenge));
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Mono.just("Error"));
    }
}
