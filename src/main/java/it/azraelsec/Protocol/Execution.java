package it.azraelsec.Protocol;

/**
 * The {@code Execution} interface is implemented by {@code Commands} handlers.
 *
 * @author Federico Gerardi
 * @author https://azraelsec.github.io/
 */
public interface Execution {
    void run(Object [] args, Result result);
}
