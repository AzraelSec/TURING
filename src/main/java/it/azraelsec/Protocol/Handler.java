package it.azraelsec.Protocol;

/**
 * The {@code Handler} interface represents a {@code Commands} handler.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public interface Handler {

    /**
     * It executes the handler function passing a {@code String} which makes sense related
     * to the {@code Commands} execution itself.
     *
     * @param msg   generic execution result string
     */
    void handle(String msg);
}
