package it.azraelsec.Protocol;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code Communication} class encapsulates all the logic related to the communication protocol used
 * to exchange commands and their execution results in a solid way.
 * <p>
 * The underline protocol is based on a request/response logic: the {@code Client} makes a request using
 * a proper {@code Commands}, the {@code Server} looks for a related handler and execute it. After that,
 * the handler itself, sends the result back using the same approach: using the {@code SUCCESS} and
 * {@code FAILURE} values from the {@code Commands} enumeration.
 * <p>
 * Each method below, before to send the raw data over TCP connection, checks for formal error in the
 * parameters' kind and number, so that can be thrown a proper {@code Exception}.
 * <p>
 * The underlining logic is simple: every data is sent through the TCP socket in a raw format, using byte
 * direct representation. When a {@code String} data is sent, before is stored an {@code Integer}
 * representing its size. This way, the receive can deduce how many bytes it needs to load from the
 * input buffer to reconstruct the original {@code String} information.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public class Communication {
    private static final int SEGMENT_SIZE = 10;
    private static Map<Commands, Class<?>[]> commandsArgsType = new HashMap<>();
    static {
        commandsArgsType.put(Commands.LOGIN, new Class<?>[] {String.class, String.class, Integer.class});
        commandsArgsType.put(Commands.LOGOUT, new Class<?>[] {});
        commandsArgsType.put(Commands.FAILURE, new Class<?>[] {String.class});
        commandsArgsType.put(Commands.SUCCESS, new Class<?>[] {String.class});
        commandsArgsType.put(Commands.EDIT, new Class<?>[] {String.class, Integer.class});
        commandsArgsType.put(Commands.EDIT_END, new Class<?>[] {});
        commandsArgsType.put(Commands.CREATE, new Class<?>[] {String.class, Integer.class});
        commandsArgsType.put(Commands.SHOW_SECTION, new Class<?>[] {String.class, Integer.class});
        commandsArgsType.put(Commands.SHOW_DOCUMENT, new Class<?>[] {String.class});
        commandsArgsType.put(Commands.LIST, new Class<?>[] {});
        commandsArgsType.put(Commands.SHARE, new Class<?>[] {String.class, String.class});
        commandsArgsType.put(Commands.NEW_NOTIFICATIONS, new Class<?>[] {String.class});
        commandsArgsType.put(Commands.EXIT, new Class<?>[] {});
    }

    /**
     * It sends a {@code Commands} which represents the action We would like to request and waits for a resulting
     * data stream.
     * <p>
     * It is useful to read file, which can be streamed without the necessity to know in advance the streamed data
     * size.
     *
     * @param outputStream  requester output stream
     * @param inputStream   requester input stream
     * @param onSuccess {@code Commands#SUCCESS} handler
     * @param stream    output stream
     * @param onFailure {@code Commands#FAILURE} handler
     * @param command   {@code Commands} to execute
     * @param args  command arguments
     */
    public static void sendAndReceiveStream(DataOutputStream outputStream, DataInputStream inputStream, Handler onSuccess, OutputStream stream, Handler onFailure, Commands command, Object...args) {
        send(outputStream, inputStream, (str) -> {
            try {
                readFileFromSocket(inputStream, stream);
                onSuccess.handle(str);
            } catch (IOException ex)
            {
                onFailure.handle(ex.getMessage());
            }
        }, onFailure, command, args);
    }

    /**
     * Receives a {@code Commands} and sends a stream back.
     *
     * @param inputStream   requester input stream
     * @param outputStream  requester output stream
     * @param tosendStream  stream to send
     * @throws IOException  if an I/O error occurs
     */
    public static void receiveAndSendStream(DataInputStream inputStream, DataOutputStream outputStream, InputStream tosendStream) throws IOException {
        byte[] buffer = new byte[SEGMENT_SIZE];
        int read;
        while((read = tosendStream.read(buffer, 0, SEGMENT_SIZE)) >= 0) {
            outputStream.writeInt(read);
            outputStream.write(buffer, 0, read);
        }
        outputStream.writeInt(-1);
    }

    /**
     * Requests a {@code Commands} execution and waits to receive its result back.
     *
     * @param outputStream  requester output stream
     * @param inputStream   requester input stream
     * @param onSuccess {@code Commands#SUCCESS} handler
     * @param onFailure {{@code Commands#FAILURE} handler}
     * @param command   {@code Commands} to execute
     * @param args  command arguments
     */
    public static void send(DataOutputStream outputStream, DataInputStream inputStream, Handler onSuccess, Handler onFailure, Commands command, Object...args) {
        try {
            Class<?>[] argsType = commandsArgsType.get(command);
            if(argsType.length != args.length) throw new IllegalArgumentException("Wrong arguments number: " + argsType.length);
            for(int i = 0; i < argsType.length; i++)
                if(!argsType[i].isAssignableFrom(args[i].getClass()))
                    throw new IllegalArgumentException("Parameter number " + i + " should have been of type " + argsType[i].getSimpleName());
            outputStream.writeInt(command.getCode());
            for(Object arg : args) {
                if(arg instanceof Integer)
                    outputStream.writeInt((Integer)arg);
                else if(arg instanceof String) {
                    byte[] bytes = ((String) arg).getBytes();
                    outputStream.writeInt(bytes.length);
                    outputStream.write(bytes);
                }
            }
            outputStream.flush();

            Map<Commands, Execution> rets = new HashMap<>();
            rets.put(Commands.SUCCESS, new ExecutionImpl(onSuccess));
            rets.put(Commands.FAILURE, new ExecutionImpl(onFailure));
            if( inputStream != null ) receive(inputStream, null, rets);
        }
        catch (Exception ex) {
            if(onFailure != null) onFailure.handle(ex.getMessage());
            else ex.printStackTrace();
        }
    }

    /**
     * Receives a {@code Commands} requests, handles it using the dispatcher pointed out as argument and sends the result
     * back through a {@code Commands} instance.
     *
     * @param inputStream   requester input stream
     * @param outputStream  requester output stream
     * @param dispatcher    (commands, handler) map
     */
    public static void receive(DataInputStream inputStream, DataOutputStream outputStream, Map<Commands, Execution> dispatcher) {
        try {
            int code = inputStream.readInt();
            Commands command = Commands.getCommand(code);
            Class<?>[] argsType = commandsArgsType.get(command);
            Object[] args = new Object[argsType.length];
            for(int i = 0; i < args.length; i++) {
                if(argsType[i] == Integer.class) args[i] = inputStream.readInt();
                else if(argsType[i] == String.class) {
                    int length = inputStream.readInt();
                    byte[] buffer = new byte[length];
                    inputStream.read(buffer);
                    args[i] = new String(buffer);
                }
            }
            dispatcher.get(command).run(args, (state, result) -> {
                if(outputStream != null) send(outputStream, null, null, null,state, result);
            });
        }
        catch (Exception ex) {
            dispatcher.get(Commands.FAILURE).run(new Object[]{ex.getMessage()}, null);
            if(outputStream == null) ex.printStackTrace();
            else send(outputStream, null, null, null, Commands.FAILURE, ex.getMessage());
        }
    }

    /**
     * Reads a file from a {@code DataInputStream} using the same approach used for the {@code String} objects
     * transferring, so sending the buffer size before as an integer data type.
     *
     * @param inputStream   requester input stream
     * @param stream    output stream
     * @throws IOException  if I/O error occurs
     */
    public static void readFileFromSocket(DataInputStream inputStream, OutputStream stream) throws IOException {
        byte[] buffer = new byte[SEGMENT_SIZE];
        int size;
        while((size = inputStream.readInt()) >= 0) {
            while(size > 0) {
                int read = inputStream.read(buffer, 0, size);
                stream.write(buffer, 0, read);
                size -= read;
            }
        }
    }
}
