package it.azraelsec.Chat;

import it.azraelsec.Document.Document;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@code CDAManager} class is a Chat Dynamic Address Manager and its task is to
 * dynamically generate and assign multicast IPv4 addresses when the {@code Server} needs it.
 * <p>
 * It stores all the records ({@code Document}, {@code Long}) (where {@code Long} is the group
 * IPv4 expressed in decimal) and when a request comes, it looks for a pre-existent row
 * assigned to that Document; if it cannot find it, creates a new one and looks for the first
 * free address (using and incremental approach), sending it back to the requester.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public class CDAManager {
    /*
    BASE_MULTICAST_ADDR corresponds to the address 239.0.0.1
    BOUND_MULTICAST_ADDR corresponds to the address 239.255.255.254
     */
    private static final long BASE_MULTICAST_ADDR = 4009754625L;
    private static final long BOUND_MULTICAST_ADDR = 4026531838L;

    private final ConcurrentHashMap<Document, Long> chatDatabase;

    /**
     * Initializes the {@code ChatMessage} internal {@code ConcurrentHashMap}
     */
    public CDAManager() {
        chatDatabase = new ConcurrentHashMap<>();
    }

    //return -1 if no address is available

    /**
     * Returns the {@code long} value corresponding to the pre-existent group
     * IPv4 address or the new one based on the number of the actual editors
     * on the {@code Document} required.
     * <p>
     * If no address are available in the multicast address space, the return
     * value will be set to (-1).
     *
     * This
     * @param document  the {@code Document} the requester is editing
     * @return  the document's multicast group IPv4 address or (-1) in case of error
     */
    public long getChatAddress(Document document) {
        Long lookedup = null;
        if((lookedup = chatDatabase.get(document)) != null) return lookedup;
        for(lookedup = BASE_MULTICAST_ADDR; lookedup <= BOUND_MULTICAST_ADDR; lookedup++)
            if(!chatDatabase.contains(lookedup)) {
                chatDatabase.put(document, lookedup);
                return lookedup;
            }
        return -1L;
    }

    /**
     * Checks if the {@code Document} in argument exists within the assigned
     * addresses' list and, in this case, it removes it.
     * @param document  to remove document
     */
    public void checkRemove(Document document) {
        Long lookedup = chatDatabase.get(document);
        if(lookedup != null)
            if(document.getOnEditingSections().length == 0)
                chatDatabase.remove(document);
    }

    /**
     * Converts an {@code InetAddress} object to its decimal representation.
     * <p>
     * This method has not been used except to test the {@code decimalToAddress} method.
     * <p>
     * It basically shifts two bytes of the IPv4 raw representation and
     * gets the value casting it to long. Then sums all together obtaining
     * the resulting value. The logical AND operator (& 0xFFL) is there to
     * avoid the sign extension caused by the conversion.
     *
     * @param address   the converted address
     * @return  the decimal representation of the input address
     */
    public static long addressToDecimal(InetAddress address) {
        byte[] rawAddress = address.getAddress();
        return ((rawAddress[0] & 0xFFL) << (3 * 8)) +
                ((rawAddress[1] & 0xFFL) << (2 * 8)) +
                ((rawAddress[2] & 0xFFL) << (8)) +
                (rawAddress[3] & 0xFFL);
    }

    /**
     * Converts a decimal value to its {@code InetAddress} representation.
     * @param address   the decimal value to convert
     * @return  the converted address
     * @throws UnknownHostException if address is not a valid value
     */
    static InetAddress decimalToAddress(long address) throws UnknownHostException {
        return InetAddress.getByName(String.valueOf(address));
    }
}

