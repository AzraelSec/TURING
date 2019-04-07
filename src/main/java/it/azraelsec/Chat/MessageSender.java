package it.azraelsec.Chat;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

/**
 * The {@code MessageSender} class's purposes is to represent an agent which
 * interacts with other {@code MessageReceiver} sending messages to them.
 * <p>
 * Cause its nature, it's not required to store the multicast group to which
 * the messages are sent, so this information is passed every time the {@code sendMessage}
 * method is called.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public class MessageSender {
    private final DatagramChannel channel;
    private final ByteBuffer buffer;

    /**
     * It stores the reference to the {@code DatagramChannel} object and allocate the internal
     * buffer used as destination for the receiving data.
     *
     * @param channel   UDP data channel
     */
    private MessageSender(DatagramChannel channel) {
        this.channel = channel;
        buffer = ByteBuffer.allocate(2048);
    }

    /**
     * It allocate a new {@code MessageSender} object, initializes the UDP channel and sets multicast interface.
     *
     * @return  a new {@code MessageSender} instance or null in case of error
     */
    public static MessageSender create() {
        try {
            DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET);
            NetworkInterface interf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, interf);
            return new MessageSender(channel);
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * It formats the data into the buffer that will be streamed in a way that the other {@code MessageReceiver}
     * can interpret.
     * <p>
 *     Every object in the buffer is stored preceded by its length and it is stored in a byte raw format.
     *
     * @param message   the message text content
     * @param group     the multicast group address
     * @throws IOException  if a I/O error occurs
     */
    public void sendMessage(ChatMessage message, InetSocketAddress group) throws IOException {
        buffer.clear();
        buffer.putInt(message.getSender().length());
        buffer.put(message.getSender().getBytes());
        buffer.putInt(message.getMessage().length());
        buffer.put(message.getMessage().getBytes());
        buffer.putLong(message.getTime());
        buffer.flip();
        while (buffer.hasRemaining())
            channel.send(buffer, group);

    }
}
