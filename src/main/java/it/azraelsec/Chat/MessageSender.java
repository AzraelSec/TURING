package it.azraelsec.Chat;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class MessageSender {
    private final DatagramChannel channel;
    private final ByteBuffer buffer;

    private MessageSender(DatagramChannel channel) {
        this.channel = channel;
        buffer = ByteBuffer.allocate(2048);
    }

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
