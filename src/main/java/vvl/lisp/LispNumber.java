package vvl.lisp;

/**
 * A number (either an integer or a real number).
 * 
 * @author leberre
 * 
 */
public class LispNumber implements LispItem {

	private Number element;

	public LispNumber(Number element) {
		this.element = element;
	}

	public Number value() {
		return element;
	}
	
	@Override
	public String toString() {
		return element.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof LispNumber) {
			LispNumber e = (LispNumber) o;
			return element.equals(e.element);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return element.hashCode();
	}
	
	public double doubleValue() {
		return  this.value().doubleValue();
	}
	
	public int compareTo(LispNumber nb) {
		double thisLispNumber = doubleValue();
		double otherLispNumber = nb.doubleValue();
		return (int) (thisLispNumber - otherLispNumber);
	}
	
}
