package it.azraelsec.Chat;

import java.io.IOException;
import java.net.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MessageReceiver extends Thread {
    public static final int CHAT_PORT = 1338;
    private final List<ChatMessage> messageQueue;
    private byte[] activeGroup;
    private MulticastSocket socket;
    private boolean closing;

    public MessageReceiver() {
        messageQueue = new ArrayList<>();
        activeGroup = null;
        socket = null;
        closing = false;
    }

    @Override
    public void run() {
        System.setProperty("java.net.preferIPv4Stack", "true");
        try {
            Selector selector = Selector.open();
            DatagramChannel channel = DatagramChannel.open();
            NetworkInterface interf = NetworkInterface.getByName("127.0.0.1");
            channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, interf);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            channel.bind(new InetSocketAddress(CHAT_PORT));
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
            ByteBuffer buffer = ByteBuffer.allocate(2048);
            while (!closing) {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isReadable()) {
                        buffer.clear();
                        InetSocketAddress sa = (InetSocketAddress) channel.receive(buffer);
                        if (sa != null) {
                            buffer.flip();
                            try {
                                String username = getString(buffer);
                                String text = getString(buffer);
                                long timestamp = buffer.getLong();
                                ChatMessage message = new ChatMessage(username, text, timestamp);
                                synchronized (messageQueue) {
                                    messageQueue.add(message);
                                }
                            } catch (BufferUnderflowException ignore) {
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace(); //todo: remove
        }
    }

    private String getString(ByteBuffer buffer) {
        int size = buffer.getInt();
        byte[] res = new byte[size];
        buffer.get(res, 0, size);
        return new String(res);
    }

    public ChatMessage[] getMessages() {
        ChatMessage[] messages = null;
        synchronized (messageQueue) {
            messages = messageQueue.toArray(new ChatMessage[0]);
            messageQueue.clear();
        }
        return messages;
    }

    public void setNewGroup(byte[] group) throws UnknownHostException, IOException {
        if (socket != null) {
            if (activeGroup != null)
                socket.leaveGroup(InetAddress.getByAddress(activeGroup));
            socket.joinGroup(InetAddress.getByAddress(group));
        }
    }

    public void shutdown() {
        closing = true;
    }
}
