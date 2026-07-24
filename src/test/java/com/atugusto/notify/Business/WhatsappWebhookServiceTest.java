package com.atugusto.notify.Business;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atugusto.notify.DTO.WebhookWhatsapp;
import com.atugusto.notify.DTO.messageTO;
import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.Entity.Platos.Categoria;
import com.atugusto.notify.Message.MensajeConfirmacion;
import com.atugusto.notify.Service.MenuMemoryService;
import com.atugusto.notify.Service.MessageService;
import com.atugusto.notify.Service.PlatosService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class WhatsappWebhookServiceTest {

    @Mock
    private MessageService messageService;

    @Mock
    private PlatosService platosService;

    @Mock
    private MenuMemoryService menuMemoryService;

    private WhatsappWebhookService service;

    @BeforeEach
    void setUp() {
        service = new WhatsappWebhookService(messageService, new ObjectMapper(), platosService, menuMemoryService);
    }

    @Test
    void processWebhookSendsMenuWhenTextRequestsIt() {
        WebhookWhatsapp payload = textPayload("111222333", "51999999999", "Hola");
        when(messageService.sendMessage(any(messageTO.class))).thenReturn(Mono.just("ok"));

        Duration response = StepVerifier.create(service.processWebhook(payload))
                .expectNext("EVENT_RECEIVED")
                .verifyComplete();

        System.out.println("Response duration: " + response.toMillis() + " ms");
        ArgumentCaptor<messageTO> messageCaptor = ArgumentCaptor.forClass(messageTO.class);
        verify(messageService).sendMessage(messageCaptor.capture());
        assertEquals("EVENT_RECEIVED", service.processWebhook(payload).block());
        assertEquals("111222333", messageCaptor.getValue().getPhone_number_id());
        assertEquals("51999999999", messageCaptor.getValue().getPhoneNumber());
        assertEquals("Hola", messageCaptor.getValue().getMessage());
    }

    @Test
    void processWebhookIgnoresTextThatIsNotAMenuRequest() {
        WebhookWhatsapp payload = textPayload("111222333", "51999999999", "Quiero reservar una mesa");

        String response = service.processWebhook(payload).block();

        assertEquals("EVENT_RECEIVED", response);
        verify(messageService, never()).sendMessage(any(messageTO.class));
    }

    @Test
    void processWebhookSavesSelectionAndSendsConfirmation() {
        Platos plato = new Platos(7L, "Lomo Saltado", Categoria.PRINCIPAL, "Lomo clasico", 32.0, true, 1L);
        WebhookWhatsapp payload = interactivePayload("111222333", "51999999999", "7", "Lomo Saltado", "Lomo clasico");
        when(platosService.findIDPlatos(7L)).thenReturn(Mono.just(plato));
        when(menuMemoryService.getMenu("51999999999"))
                .thenReturn(Mono.just(List.of()), Mono.just(List.of(plato)));
        when(menuMemoryService.addPlato(eq("51999999999"), eq(plato))).thenReturn(Mono.just(List.of(plato)));
        when(messageService.sendMessageConfirm(any(messageTO.class), any())).thenReturn(Mono.just("ok"));

        String response = service.processWebhook(payload).block();

        ArgumentCaptor<messageTO> messageCaptor = ArgumentCaptor.forClass(messageTO.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Platos>> platosCaptor = ArgumentCaptor.forClass(List.class);

        verify(messageService).sendMessageConfirm(messageCaptor.capture(), platosCaptor.capture());
        assertEquals("EVENT_RECEIVED", response);
        assertEquals("111222333", messageCaptor.getValue().getPhone_number_id());
        assertEquals("51999999999", messageCaptor.getValue().getPhoneNumber());
        assertSame(plato, platosCaptor.getValue().get(0));
    }

    @Test
    void processWebhookHandlesAgregarActionByResendingMenu() {
        WebhookWhatsapp payload = interactivePayload("111222333", "51999999999", MensajeConfirmacion.AGREGAR,
                "Agregar otro plato", "Volver al menu");
        when(messageService.sendMessage(any(messageTO.class))).thenReturn(Mono.just("ok"));

        String response = service.processWebhook(payload).block();

        ArgumentCaptor<messageTO> messageCaptor = ArgumentCaptor.forClass(messageTO.class);
        verify(messageService).sendMessage(messageCaptor.capture());
        verify(messageService, never()).sendMessageConfirm(any(messageTO.class), any());
        assertEquals("EVENT_RECEIVED", response);
        assertEquals("menu", messageCaptor.getValue().getMessage());
    }

    @Test
    void processWebhookCancelsOrderAndAvoidsConfirmation() {
        Platos plato = new Platos(7L, "Lomo Saltado", Categoria.PRINCIPAL, "Lomo clasico", 32.0, true, 1L);
        WebhookWhatsapp selectedDish = interactivePayload("111222333", "51999999999", "7", "Lomo Saltado", "Lomo clasico");
        when(platosService.findIDPlatos(7L)).thenReturn(Mono.just(plato));
        when(menuMemoryService.getMenu("51999999999"))
                .thenReturn(Mono.just(List.of()), Mono.just(List.of(plato)));
        when(menuMemoryService.addPlato(eq("51999999999"), eq(plato))).thenReturn(Mono.just(List.of(plato)));
        when(messageService.sendMessageConfirm(any(messageTO.class), any())).thenReturn(Mono.just("ok"));
        when(menuMemoryService.removeMenu("51999999999")).thenReturn(Mono.empty());

        service.processWebhook(selectedDish).block();

        WebhookWhatsapp cancelPayload = interactivePayload("111222333", "51999999999", MensajeConfirmacion.CANCELAR,
                "Cancelar pedido", "Vaciar pedido");
        String response = service.processWebhook(cancelPayload).block();

        assertEquals("EVENT_RECEIVED", response);
        verify(messageService).sendMessageConfirm(any(messageTO.class), any());
        verify(messageService, never()).sendMessage(any(messageTO.class));
        verify(menuMemoryService).removeMenu("51999999999");
    }

    private WebhookWhatsapp textPayload(String phoneNumberId, String from, String body) {
        WebhookWhatsapp payload = basePayload(phoneNumberId);
        WebhookWhatsapp.Message message = new WebhookWhatsapp.Message();
        message.from = from;
        message.timestamp = "1720000000";
        message.type = "text";
        message.text = new WebhookWhatsapp.Text();
        message.text.body = body;
        payload.entry.get(0).changes.get(0).value.messages = List.of(message);
        return payload;
    }

    private WebhookWhatsapp interactivePayload(String phoneNumberId, String from, String id, String title,
            String description) {
        WebhookWhatsapp payload = basePayload(phoneNumberId);
        WebhookWhatsapp.Message message = new WebhookWhatsapp.Message();
        message.from = from;
        message.timestamp = "1720000000";
        message.type = "interactive";
        message.interactive = new WebhookWhatsapp.Interactive();
        message.interactive.type = "list_reply";
        message.interactive.list_reply = new WebhookWhatsapp.ListReply();
        message.interactive.list_reply.id = id;
        message.interactive.list_reply.title = title;
        message.interactive.list_reply.description = description;
        payload.entry.get(0).changes.get(0).value.messages = List.of(message);
        return payload;
    }

    private WebhookWhatsapp basePayload(String phoneNumberId) {
        WebhookWhatsapp payload = new WebhookWhatsapp();
        WebhookWhatsapp.Entry entry = new WebhookWhatsapp.Entry();
        WebhookWhatsapp.Change change = new WebhookWhatsapp.Change();
        WebhookWhatsapp.Value value = new WebhookWhatsapp.Value();
        value.metadata = new WebhookWhatsapp.Metadata();
        value.metadata.phone_number_id = phoneNumberId;
        change.value = value;
        entry.changes = List.of(change);
        payload.entry = List.of(entry);
        return payload;
    }
}
