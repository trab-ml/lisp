package vvl.util;


/**
 * Factory to create new lists.
 * 
 * The methods take advantage of type inference to simplify the use of the
 * methods in the user code.
 * 
 * The body of the methods must be completed by the students.
 * 
 * @author leberre
 *
 */
public final class ConsListFactory {

    private ConsListFactory() {
        // do nothing
    }

    /**
     * Create a new empty list.
     * 
     * @return an empty list
     */
    public static <T> ConsList<T> nil() {
    	// TODO
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Create a new list containing a single element
     * 
     * @param t
     *            an object
     * @return a list containing only t
     */
    public static <T> ConsList<T> singleton(T t) {
    	// TODO
    	throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Create a new list containing the elements given in parameter
     * 
     * @param ts
     *            a variable number of elements
     * @return a list containing those elements
     */
    @SafeVarargs
    public static <T> ConsList<T> asList(T... ts) {
    	// TODO
    	throw new UnsupportedOperationException("Not implemented yet");
    }
}
