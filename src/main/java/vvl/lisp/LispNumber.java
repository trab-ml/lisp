package vvl.lisp;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

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
		return this.value().doubleValue();
	}

	public BigDecimal bigDecimalValue() {
		if (element instanceof BigDecimal) {
			return (BigDecimal) element;
		} else {
			return new BigDecimal(element.toString());
		}
	}
	
	public BigInteger bigIntegerValue() {
		if (element instanceof BigInteger) {
			return (BigInteger) element;
		} else {
			return new BigInteger(element.toString());
		}
	}

	public int compareTo(LispNumber nb) {
		return bigDecimalValue().compareTo(nb.bigDecimalValue());
	}

	public static BigDecimal setScale(BigDecimal bd) {
	    int scale = bd.scale();
	    if (bd.stripTrailingZeros().scale() != bd.scale()) {
	        scale = bd.stripTrailingZeros().scale();
	    }
	    scale = Math.max(scale, 1);
	    return bd.setScale(scale, RoundingMode.HALF_UP);
	}
	
	public LispNumber add(LispNumber operand) {
		if (isInteger() && operand.isInteger()) {
			BigInteger result = (this.bigIntegerValue()).add(operand.bigIntegerValue());
			return new LispNumber(result);
		}
		BigDecimal result = bigDecimalValue().add(operand.bigDecimalValue());
		return new LispNumber(result);
	}

	public LispNumber subtract(LispNumber operand) {
		if (isInteger() && operand.isInteger()) {
			BigInteger result = (this.bigIntegerValue()).subtract(operand.bigIntegerValue());
			return new LispNumber(result);
		}
		BigDecimal result = bigDecimalValue().subtract(operand.bigDecimalValue());
		return new LispNumber(result);
	}

	public LispNumber multiply(LispNumber operand) {
		if (isInteger() && operand.isInteger()) {
			BigInteger result = (this.bigIntegerValue()).multiply(operand.bigIntegerValue());
			return new LispNumber(result);
		}
		BigDecimal result = bigDecimalValue().multiply(operand.bigDecimalValue());
		return new LispNumber(setScale(result));
	}

	public LispNumber divide(LispNumber operand) throws ArithmeticException {		
		if (isInteger() && operand.isInteger()) {
			BigInteger result = (this.bigIntegerValue()).divide(operand.bigIntegerValue());
			return new LispNumber(result);
		}
		BigDecimal result = bigDecimalValue().divide(operand.bigDecimalValue());
		return new LispNumber(setScale(result));
	}

	private boolean isInteger() {
		return element instanceof Integer || element instanceof BigInteger;
	}
}
