package vvl.util;


/**
 * Building block for implementing lists.
 * 
 * A "cons" is simply a pair (L,R) holding a specific value on the left hand
 * side (L) and the right hand side (R).
 * 
 * See e.g. {@link https://en.wikipedia.org/wiki/Cons} for details.
 * 
 * @author leberre
 *
 * @param <L>
 *            the type of the left hand side of the pair
 * @param <R>
 *            the type of the right hand side of the pair
 */
public class Cons<L, R> {

    /**
     * The left hand side of the pair.
     */
    private final L left;

    /**
     * The right hand side of the pair.
     */
    private R right;

    /**
     * Private default constructor to prevent its use outside the class.
     */
    private Cons() {
        this(null, null);
    }

    /**
     * Generic constructor for the cons data structure.
     * 
     * @param left
     *            the left hand side.
     * @param right
     *            the right hand side.
     */
    public Cons(L left, R right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Retrieve the left hand side of the pair.
     * 
     * @return the left hand side of the pair if any.
     */
    public L left() {
        return left;
    }
    
    /**
     * Retrieve the right hand side of the pair.
     * 
     * @return the right hand side of the pair if any.
     */
    public R right() {
        return right;
    }

    public void setRight(R right) {
    	this.right = right;
    }
    
    /**
     * Typical textual "dotted" representation of a cons : ( L . R ).
     * 
     * @return a dotted textual representation of the cons pair.
     */
    @Override
    public String toString() {
        return "(" + left + " . " + right + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((left == null) ? 0 : left.hashCode());
        return prime * result + ((right == null) ? 0 : right.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Cons<?, ?> other = (Cons<?, ?>) obj;
        if (left == null) {
            if (other.left != null)
                return false;
        } else if (!left.equals(other.left))
            return false;
        if (right == null) {
            if (other.right != null)
                return false;
        } else if (!right.equals(other.right))
            return false;
        return true;
    }

    /**
     * Uses type inference to build an empty cons of the expected type.
     * 
     * @return an empty cons of the expected type.
     */
    public static final <U, V> Cons<U, V> nil() {
        return new Cons<>();
    }

}
