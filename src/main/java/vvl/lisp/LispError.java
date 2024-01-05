package vvl.lisp;

/**
 * Generic exception for our lisp interpreter.
 * 
 * @author leberre
 *
 */
public class LispError extends Exception {

    /**
     * Fake serial version UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Create a new exception with a message and a cause.
     * 
     * @param message
     *            a detailed message intended to the end user.
     * @param cause
     *            the reason of the exception (e.g another exception).
     */
    public LispError(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new exception with a message.
     * 
     * @param message
     *            a detailed message intended to the end user.
     */
    public LispError(String message) {
        super(message);
    }
}
