package it.azraelsec.Protocol;

/**
 * The {@code Result} interface is implemented by the class that represents a back channel useful
 * for the entity that sends a result back depending on an execution response.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public interface Result{
    /**
     * Sends back a {@code Commands} and a result {@code String} to the requester.
     * <p>
     * The {@code c} value should be {@code Commands#SUCCESS} or {@code Commands#FAILUER} and nothing different.
     *
     * @param c result {@code Commands}
     * @param r result {@code String}
     */
    void send(Commands c, String r);
}
