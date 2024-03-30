package vvl.lisp;

import vvl.util.ConsList;

/**
 * Representation of a Lisp expression (a list of LispItem).
 * 
 * @author leberre
 *
 */
public class LispExpression implements LispItem {

	private ConsList<LispItem> expression;

	public LispExpression() {
		this.expression = ConsList.nil();
	}
	
	public LispExpression(LispItem ... items ) {
		this.expression = ConsList.asList(items);
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
	
	public boolean isEmpty() {
		return this.expression.isEmpty();
	}
	
	// the item at index n
	public LispItem nth(int n) {
		int len = this.expression.size() - n;
		if (len <= 0) {
			return null;
		}
		
		ConsList<LispItem> currentExpr = this.expression;
		LispItem nthItem = null;
		while (len > 0 && !currentExpr.isEmpty()) {
			currentExpr = currentExpr.cdr();
			len--;
		}
		nthItem = currentExpr.car();
		return nthItem;
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
