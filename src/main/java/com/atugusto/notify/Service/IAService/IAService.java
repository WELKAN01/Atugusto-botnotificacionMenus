package com.atugusto.notify.Service.IAService;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import com.atugusto.notify.Service.PlatosService;


@Service
public class IAService {
    private final ChatClient chatClient;

    public IAService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }


    public String ask(String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
