package vvl.lisp;

/**
 * Representation of an identifier (a String).
 * 
 * @author leberre
 *
 */
public class LispIdentifier implements LispItem {
	
	private final String id;
	
	public LispIdentifier(String id) {
		this.id = id;
		
	}
	
	@Override
	public String toString() {
		return id;
	}
	
	@Override 
	public boolean equals(Object o) {
		if (o==null) {
			return false;
		}
		return id.equals(o.toString());
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    /*
     * Analyze if a given string is a {@link LispIdentifier}
     * @return an instance of a {@link LispIdentifier} or null
     */
    public static LispIdentifier isLispIdentifier(String s) {
    	if (s != null && !s.isEmpty()) {
			return new LispIdentifier(s);
		} else {
			return null;
		}
    }
}
