package it.azraelsec.Chat;

import it.azraelsec.Client.Client;

import java.io.IOException;
import java.net.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The {@code MessageReceiver} class represent a {@code ChatMessage} receiver
 * which waits for new messages addressed to him and stores them in a {@code List}.
 * <p>
 * The {@code Client} will then have the possibility to asyncronously withdraw
 * them all.
 * <p>
 * This {@code Thread} spawn a {@code DatagramChannel} which acts like a
 * UDP Server which waits for new multicast messages sent from other {@code Client}
 * processes around the network.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public class MessageReceiver extends Thread {
    private final List<ChatMessage> messageQueue;
    private MembershipKey activeGroup;
    private DatagramChannel channel;
    private NetworkInterface interf;

    /**
     * It initializes the internal structure related to the UDP channel and
     * message queue.
     */
    public MessageReceiver() {
        messageQueue = new ArrayList<>();
        activeGroup = null;
        channel = null;
        interf = null;
    }

    /**
     * It is the main method in which is executed all the thread control logic.
     */
    @Override
    public void run() {
        try {
            Selector selector = Selector.open();
            channel = DatagramChannel.open(StandardProtocolFamily.INET);
            interf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            channel.setOption(StandardSocketOptions.IP_MULTICAST_IF, interf);
            channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            channel.bind(new InetSocketAddress(Client.UDP_PORT));
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
            ByteBuffer buffer = ByteBuffer.allocate(2048);
            while (!Thread.currentThread().isInterrupted()) {
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
        finally {
            if(channel != null)
                try {
                    channel.close();
                } catch (IOException ignore) {
                }
        }
    }

    /**
     * It returns a String representation of the {@code ByteBuffer} content.
     * @param buffer    the source ByteBuffer
     * @return          the string source buffer's content
     */
    private String getString(ByteBuffer buffer) {
        int size = buffer.getInt();
        byte[] res = new byte[size];
        buffer.get(res, 0, size);
        return new String(res);
    }

    /**
     * It gets all the messages existing in the queue.
     * @return  the available {@code ChatMessage} array
     */
    public ChatMessage[] getMessages() {
        ChatMessage[] messages = null;
        synchronized (messageQueue) {
            messages = messageQueue.toArray(new ChatMessage[0]);
            messageQueue.clear();
        }
        return messages;
    }

    /**
     * It makes the {@code MessageReceiver} to listen for messages coming from
     * the group represented by the IPv4 address in decimal format.
     * @param group                 the group address in decimal representation
     * @throws IOException          if a I/O error occurs
     * @throws UnknownHostException if group is not a valid address
     */
    public void setNewGroup(long group) throws IOException, UnknownHostException {
        if (channel != null) {
            if (group > 0) {
                byte[] rawAddress = CDAManager.decimalToAddress(group).getAddress();
                if (activeGroup != null && activeGroup.isValid()) activeGroup.drop();
                activeGroup = channel.join(InetAddress.getByAddress(rawAddress), interf);
            }
            else {
                activeGroup.drop();
                activeGroup = null;
            }
        }
    }

    /**
     * Get the active group.
     * @return the {@code InetAddress} active group address
     */
    public InetAddress getActiveGroup() {
        return activeGroup.group();
    }
}
