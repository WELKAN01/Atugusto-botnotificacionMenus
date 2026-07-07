package com.atugusto.notify.Service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.atugusto.notify.DTO.messageTO;
import com.atugusto.notify.Entity.Empresa;
import com.atugusto.notify.Entity.Platos;
import com.atugusto.notify.Entity.Platos.Categoria;
import com.atugusto.notify.Message.MensajeConfirmacion;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

class MessageServiceTest {

    @Test
    void sendMessageFailsWhenMetaTokenIsMissing() {
        MessageService service = new MessageService(WebClient.builder().build(), "   ",
                mock(PlatosDiariosService.class));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.sendMessage(new messageTO("4361450264132587", "51970479585", "hola")).block());
        
        System.out.println("Exception message: " + exception.getMessage());
        assertEquals("META_WHATSAPP_TOKEN is not configured", exception.getMessage());
    }

    @Test
    void sendMessageTemplateWithPlatosBuildsInteractiveMenuFromDailyDishes() throws Exception {
        EmpresaService empresaService = mock(EmpresaService.class);
        when(empresaService.getOrCreateDefaultEmpresa()).thenReturn(
            Mono.just(Empresa.demo())
        );
        PlatosDiariosService platosDiariosService = mock(PlatosDiariosService.class);
        when(platosDiariosService.platosDiariosListToday()).thenReturn(Flux.fromIterable(
                List.of(
                new Platos(1L, "Lomo Saltado", Categoria.PRINCIPAL, "Carne salteada", 32.5, true,1L),
                new Platos(2L, "Aji de Gallina", Categoria.PRINCIPAL, "Crema de aji amarillo", 28.0, true,1L))
            ));
        MessageService service = new MessageService(WebClient.builder().build(), "token",
                platosDiariosService);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) invokePrivate(service, "sendMessageTemplateWithPlatos",
                new messageTO("4361450264132587", "51970479585", "menu"));

        assertEquals("whatsapp", body.get("messaging_product"));
        assertEquals("51970479585", body.get("to"));
        assertEquals("interactive", body.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> interactive = (Map<String, Object>) body.get("interactive");
        @SuppressWarnings("unchecked")
        Map<String, Object> action = (Map<String, Object>) interactive.get("action");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sections = (List<Map<String, Object>>) action.get("sections");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> rows = (List<Map<String, String>>) sections.get(0).get("rows");

        assertEquals(2, rows.size());
        assertEquals("1", rows.get(0).get("id"));
        assertEquals("Lomo Saltado", rows.get(0).get("title"));
        assertTrue(rows.get(0).get("description").contains("Carne salteada"));
        assertTrue(rows.get(0).get("description").contains("32.50"));
    }

    @Test
    void sendMessageTemplateConfirmedBuildsConfirmationOptions() throws Exception {
        EmpresaService empresaService = mock(EmpresaService.class);
        when(empresaService.getOrCreateDefaultEmpresa()).thenReturn(
            Mono.just(Empresa.demo())
        );
        
        MessageService service = new MessageService(WebClient.builder().build(), "token",
                mock(PlatosDiariosService.class));

        Map<String, List<Platos>> memory = Map.of(
                "51970479585", List.of(
                        new Platos(1L, "Lomo Saltado", Categoria.PRINCIPAL, "Carne salteada", 32.5, true,1L),
                        new Platos(2L, "Aji de Gallina", Categoria.PRINCIPAL, "Crema de aji amarillo", 28.0, true,1L)));

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) invokePrivate(service, "sendMessageTemplateConfirmed",
                new messageTO("4361450264132587", "51970479585", null), memory);

        @SuppressWarnings("unchecked")
        Map<String, Object> interactive = (Map<String, Object>) body.get("interactive");
        @SuppressWarnings("unchecked")
        Map<String, String> bodyText = (Map<String, String>) interactive.get("body");
        @SuppressWarnings("unchecked")
        Map<String, Object> action = (Map<String, Object>) interactive.get("action");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sections = (List<Map<String, Object>>) action.get("sections");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> rows = (List<Map<String, String>>) sections.get(0).get("rows");

        assertTrue(bodyText.get("text").contains("Carne salteada"));
        assertTrue(bodyText.get("text").contains("Crema de aji amarillo"));
        assertEquals(MensajeConfirmacion.obtenerLista(), rows);
    }

    private Object invokePrivate(Object target, String methodName, Object... args) {
        try {
            Method method = findMethod(target.getClass(), methodName, args.length);
            if (method == null) {
                throw new NoSuchMethodException(methodName);
            }
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            throw new RuntimeException(exception.getCause());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private Method findMethod(Class<?> type, String methodName, int parameterCount) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && method.getParameterCount() == parameterCount) {
                return method;
            }
        }
        return null;
    }
}
