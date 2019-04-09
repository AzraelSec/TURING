package it.azraelsec.Chat;

import java.io.Serializable;
import java.util.Date;

/**
 * The {@code ChatMessage} class represents a multicast chat message sent from
 * {@code MessageSender} or received by {@code MessageReceiver}.
 * <p>
 * It stores the user who send it, the message itself and the timestamp of the
 * moment it has been sent.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public class ChatMessage implements Serializable, Comparable<ChatMessage> {
    private static final long serialVersionUID = 1L;
    private final String sender;
    private final String message;
    private final long time;

    /**
     * It stores the data the message contains and initializes the timestamp.
     * @param sender    the {@code String} sender username
     * @param message   the {@code String} message text
     */
    public ChatMessage(String sender, String message) {
        this.sender = sender;
        this.message = message;
        time = new Date().getTime();
    }

    /**
     * It stores the data the message contains.
     * @param sender    the {@code String} sender username
     * @param message   the {@code String} message text
     * @param time      the {@code long} message timestamp
     */
    public ChatMessage(String sender, String message, long time) {
        this.sender = sender;
        this.message = message;
        this.time = time;
    }

    /**
     * Get the message sender.
     * @return  the sender username
     */
    public String getSender() {
        return sender;
    }

    /**
     * Get the message text.
     * @return the message text
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the message sending timestamp.
     * @return the sending timestamp long representation
     */
    public long getTime() {
        return time;
    }

    /**
     * Manage the printable message version.
     * @return  return a printable message version
     */
    @Override
    public String toString() {
        return "[" + sender +"] - " + message;
    }

    /**
     * Makes two {@code ChatMessage}s (horribly) comparable based on the timestamp value.
     *
     * @param o object to compare the timestamp to
     * @return  a value based on the difference between their timestamp
     */
    @Override
    public int compareTo(ChatMessage o) {
        return (int) (time - o.getTime());
    }
}
