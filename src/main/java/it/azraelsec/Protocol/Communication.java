package it.azraelsec.Protocol;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Communication {
    private static final int SEGMENT_SIZE = 10;
    private static Map<Commands, Class<?>[]> commandsArgsType = new HashMap<>();
    static {
        commandsArgsType.put(Commands.LOGIN, new Class<?>[] {Integer.class, String.class, String.class});
        commandsArgsType.put(Commands.LOGOUT, new Class<?>[] {});
        commandsArgsType.put(Commands.FAILURE, new Class<?>[] {String.class});
        commandsArgsType.put(Commands.SUCCESS, new Class<?>[] {String.class});
        commandsArgsType.put(Commands.EDIT, new Class<?>[] {String.class, Integer.class});
        commandsArgsType.put(Commands.EDIT_END, new Class<?>[] {});
        commandsArgsType.put(Commands.CREATE, new Class<?>[] {String.class, Integer.class});
        commandsArgsType.put(Commands.SHOW_SECTION, new Class<?>[] {String.class, Integer.class});
        commandsArgsType.put(Commands.SHOW_DOCUMENT, new Class<?>[] {String.class});
    }

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

    public static void receiveAndSendStream(DataInputStream inputStream, DataOutputStream outputStream, InputStream tosendStream) throws IOException {
        byte[] buffer = new byte[SEGMENT_SIZE];
        int read;
        while((read = tosendStream.read(buffer, 0, SEGMENT_SIZE)) >= 0) {
            outputStream.writeInt(read);
            outputStream.write(buffer, 0, read);
        }
        outputStream.writeInt(-1);
    }

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
            Optional.ofNullable(onFailure).ifPresentOrElse((lambda) -> lambda.handle(ex.getMessage()), ex::printStackTrace);
        }
    }

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
