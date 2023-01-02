package vvl.lisp;

/**
 * Simple Boolean type for our lisp interpreter.
 * 
 * We do not use the classical boolean operators in order to have an easy
 * translation to and from the lisp representation of Booleans.
 * 
 * The Boolean operators are not implemented. They must be applied on the
 * {@link #value()} of the object.
 * 
 * @author leberre
 *
 */
public final class LispBoolean {

    /**
     * The object representing the true Boolean value.
     */
    public static final LispBoolean TRUE = new LispBoolean(true);

    /**
     * The object representing the false Boolean value.
     */
    public static final LispBoolean FALSE = new LispBoolean(false);

    private final boolean value;

    private LispBoolean(boolean value) {
        this.value = value;
    }

    /**
     * Retrieve a Java primitive Boolean value for Boolean reasoning in Java code.
     * 
     * @return the corresponding Java's Boolean value.
     */
    public boolean value() {
        return this.value;
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LispBoolean) {
            return ((LispBoolean) obj).value == value;
        }
        return false;
    }

    @Override
    public String toString() {
        return value ? "#t" : "#f";
    }

    /**
     * Retrieve a {@link LispBoolean} from a Java primitive Boolean.
     * 
     * @param b
     *            a Boolean value
     * @return the corresponding {@link LispBoolean} object.
     */
    public static LispBoolean valueOf(boolean b) {
        return b ? TRUE : FALSE;
    }

    /**
     * Retrieve a {@link LispBoolean} from its textual representation.
     * 
     * @param s
     *            a textual representation of the boolean value (#t or #f)
     * @return the corresponding {@link LispBoolean} object.
     * @throws IllegalArgumentException
     *             if s does not correspond to a valid textual representation.
     */
    public static LispBoolean valueOf(String s) {
        switch (s.toLowerCase()) {
        case "#t":
            return TRUE;
        case "#f":
            return FALSE;
        default:
            throw new IllegalArgumentException("Not a Boolean");
        }
    }
}
