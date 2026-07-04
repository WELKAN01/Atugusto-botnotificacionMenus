package com.atugusto.notify.Message;

import java.util.List;
import java.util.Map;

public final class MensajeConfirmacion {
    public static final String CONFIRMAR = "CONFIRMAR";
    public static final String AGREGAR = "AGREGAR";
    public static final String CANCELAR = "CANCELAR";

    private MensajeConfirmacion() {
    }

    public static List<Map<String, String>> obtenerLista() {
        return List.of(
                Map.of(
                        "id", CONFIRMAR,
                        "title", "Confirmar pedido"
                ),
                Map.of(
                        "id", AGREGAR,
                        "title", "Agregar otro plato"
                ),
                Map.of(
                        "id", CANCELAR,
                        "title", "Cancelar pedido"
                )
        );
    }
}
