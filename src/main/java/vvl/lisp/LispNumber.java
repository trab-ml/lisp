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

	public int compareTo(LispNumber nb) {
		BigDecimal thisLispNumber = bigDecimalValue();
		BigDecimal otherLispNumber = nb.bigDecimalValue();
		return thisLispNumber.compareTo(otherLispNumber);
	}

	/* helpers functions for arithmetic operations */
	public LispNumber add(LispNumber operand) {
		if (isInteger() && operand.isInteger()) {
			int result = value().intValue() + operand.value().intValue();
			return new LispNumber(result);
		} else {
			BigDecimal result = bigDecimalValue().add(operand.bigDecimalValue());
			return new LispNumber(result.setScale(1, RoundingMode.HALF_UP));
		}
	}

	public LispNumber subtract(LispNumber operand) {
		if (isInteger() && operand.isInteger()) {
			int result = value().intValue() - operand.value().intValue();
			return new LispNumber(result);
		} else {
			BigDecimal result = bigDecimalValue().subtract(operand.bigDecimalValue());
			return new LispNumber(result.setScale(1, RoundingMode.HALF_UP));
		}
	}

	public LispNumber multiply(LispNumber operand) {
		if (isInteger() && operand.isInteger()) {
			int result = value().intValue() * operand.value().intValue();
			return new LispNumber(result);
		} else {
			BigDecimal result = bigDecimalValue().multiply(operand.bigDecimalValue());
			return new LispNumber(result.setScale(1, RoundingMode.HALF_UP));
		}
	}

	public LispNumber divide(LispNumber operand) {
		if (isInteger() && operand.isInteger()) {
			if (operand.value().intValue() == 0) {
				throw new ArithmeticException("Division by zero");
			}
			int result = value().intValue() / operand.value().intValue();
			return new LispNumber(result);
		} else {
			if (operand.compareTo(new LispNumber(BigInteger.ZERO)) == 0) {
				throw new ArithmeticException("Division by zero");
			}
			BigDecimal result = bigDecimalValue().divide(operand.bigDecimalValue());
			return new LispNumber(result.setScale(1, RoundingMode.HALF_UP));
		}
	}

	private boolean isInteger() {
		return element instanceof Integer || element instanceof BigInteger;
	}
}
