package it.azraelsec.Protocol;

/**
 * The {@code ExecutionImpl} class implements {@code Execution} interface, representing a {@code Commands}
 * execution.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public class ExecutionImpl implements Execution {
    private Handler handler;

    /**
     * Initializes the {@code ExecutionImpl} object.
     *
     * @param handler   the related {@code Handler}
     */
    ExecutionImpl(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void run(Object[] args, Result result) {
        if (handler != null) handler.handle((String) args[0]);
    }
}
