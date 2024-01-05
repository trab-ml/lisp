package vvl.lisp;

import vvl.util.ConsList;
import vvl.util.ConsListFactory;

/**
 * Representation of a Lisp expression (a list of LispItem).
 * 
 * @author leberre
 *
 */
public class LispExpression implements LispItem {

	private ConsList<LispItem> expression;

	public LispExpression() {
		this.expression = ConsListFactory.nil();
	}
	
	public LispExpression(LispItem ... items ) {
		this.expression = ConsListFactory.asList(items);
	}

	public void prepend(LispItem item) {
		this.expression = this.expression.prepend(item);
	}
	
	public void append(LispItem item) {
		this.expression = this.expression.append(item);
	}
	
	public ConsList<LispItem> values() {
		return expression;
	}

	@Override
	public String toString() {
		return expression.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof LispExpression) {
			LispExpression l = (LispExpression)o;
			return expression.equals(l.expression);
		}
		return false;
    }
    
    @Override
    public int hashCode() {
        return expression.hashCode();
    }    
}
