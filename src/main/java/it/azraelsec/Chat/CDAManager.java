package it.azraelsec.Chat;

import it.azraelsec.Documents.Document;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

//Chat Dynamic Address Manager
public class CDAManager {
    public static final long BASE_MULTICAST_ADDR = 4009754625L; //239.0.0.1
    public static final long BOUND_MULTICAST_ADDR = 4026531838L; //239.255.255.254

    private final ConcurrentHashMap<Document, Long> chatDatabase;

    public CDAManager() {
        chatDatabase = new ConcurrentHashMap<>();
    }

    //return -1 if no address is available
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

    public void checkRemove(Document document) {
        Long lookedup = chatDatabase.get(document);
        if(lookedup != null)
            if(document.getOnEditingSections().length == 0)
                chatDatabase.remove(document);
    }

    public static long addressToDecimal(InetAddress address) {
        byte[] rawAddress = address.getAddress(); //get raw address
        return ((rawAddress[0] & 0xFFL) << (3 * 8)) + //& 0xff blocks sign extension
                ((rawAddress[1] & 0xFFL) << (2 * 8)) +
                ((rawAddress[2] & 0xFFL) << (8)) +
                (rawAddress[3] & 0xFFL);
    }

    public static InetAddress decimalToAddress(long address) throws UnknownHostException {
        return InetAddress.getByName(String.valueOf(address));
    }
}

