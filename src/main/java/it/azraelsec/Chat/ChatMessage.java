package it.azraelsec.Chat;

import java.io.Serializable;
import java.util.Date;

public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String sender;
    private final String message;
    private final long time;

    public ChatMessage(String sender, String message) {
        this.sender = sender;
        this.message = message;
        time = new Date().getTime();
    }

    public ChatMessage(String sender, String message, long time) {
        this.sender = sender;
        this.message = message;
        this.time = time;
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public long getTime() {
        return time;
    }

    @Override
    public String toString() {
        return "[" + sender +"] - " + message;
    }
}
