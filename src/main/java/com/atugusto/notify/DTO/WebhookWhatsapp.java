package com.atugusto.notify.DTO;

import java.util.List;

public class WebhookWhatsapp {
    public List<Entry> entry;

    public static class Entry {
        public String id;
        public List<Change> changes;
    }

    public static class Change {
        public Value value;
    }

    public static class Value {
        public Metadata metadata;
        public List<Message> messages;
        public List<Status> statuses;
    }

    public static class Metadata {
        public String display_phone_number;
        public String phone_number_id;
    }

    public static class Message {
        public String from;
        public String timestamp;
        public String type;        // "text", "interactive", etc.
        public Text text;
        public Interactive interactive;  // ← AGREGA ESTO
    }

    public static class Text {
        public String body;
    }

    // ── Clases nuevas para interactive ──────────────────

    public static class Interactive {
        public String type;               // "list_reply" o "button_reply"
        public ListReply list_reply;
        public ButtonReply button_reply;
    }

    public static class ListReply {
        public String id;                 // ID del plato elegido
        public String title;              // Nombre del plato
        public String description;
    }

    public static class ButtonReply {
        public String id;                 // "confirmar_pedido", "ver_menu", "cancelar"
        public String title;
    }

    // ────────────────────────────────────────────────────

    public static class Status {
        public String id;
        public String status;
        public String timestamp;
    }
}
