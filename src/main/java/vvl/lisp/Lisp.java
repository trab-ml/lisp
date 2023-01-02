package vvl.lisp;

/**
 * A simple abstraction for a lisp interpreter.
 * 
 * @author leberre
 *
 */
public interface Lisp {

    /**
     * Parse a textual lisp expression and cut it into operator and operands for
     * further evaluation.
     * 
     * @param expr
     * @return a single object or a list of elements
     * @throws LispError
     *             if the expression is not a valid Lisp/scheme expression
     */
    Object parse(String expr) throws LispError;

    /**
     * Evaluate the expression returned by the {@link #parse(String)} method.
     * 
     * @param ex
     *            the result of the {@link #parse(String)} method
     * @return a lisp evaluation of the parameter
     * @throws LispError
     *             if the expression cannot be evaluated
     */
    Object evaluate(Object ex) throws LispError;

    /**
     * Evaluate a lisp expression.
     * 
     * @param expr
     *            a lisp expression
     * @return a
     * @throws LispError
     *             if the expression is malformed or cannot be evaluated.
     */
    default Object eval(String expr) throws LispError {
        return evaluate(parse(expr));
    }
}
