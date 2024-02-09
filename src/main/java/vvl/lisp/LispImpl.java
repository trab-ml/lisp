package vvl.lisp;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LispImpl implements Lisp {

	@Override
	public LispItem parse(String expr) throws LispError {
		try {
			LispParser parser = new LispParser(expr);
			LispItem parsed = parser.parse();

			while (!parser.isInputEmpty()) {
				parsed = parser.parse();
			}

			if (parser.getParsedLispItems().size() == 2) {
				throw new LispError("Unexpected data after parsing complete!");
			}

			return parsed;
		} catch (Exception e) {
			throw new LispError("Invalid number of operands");
		}
	}

	/**
	 * Parse a given string and turn it into LispItems.
	 */
	private static class LispParser {

		private final String input;
		private int index = 0;
		private ArrayList<LispItem> lispItemsList = new ArrayList<LispItem>();
		private int exprNestingLevel = 0;

		public LispParser(String input) {
			this.input = input.trim();
		}

		public ArrayList<LispItem> getParsedLispItems() {
			return lispItemsList;
		}

		public boolean isInputEmpty() {
			return index >= input.length();
		}

		private void insideAnExpr(LispItem it) {
			if (exprNestingLevel == 0)
				lispItemsList.add(it);
		}

		private boolean numberFollowsOperator(String in, Character c, int i) {
			return c == '-' && i + 1 < in.length() && Character.isDigit(in.charAt(i + 1));
		}

		private LispItem parse() throws LispError {
			skipWhiteSpace();
			if (index >= input.length()) {
				throw new LispError("Unauthorized expression");
			}

			char firstChar = input.charAt(index);
			LispItem result;

			switch (firstChar) {
			case '#':
				result = parseBoolean();
				insideAnExpr(result);
				break;

			case '(':
				exprNestingLevel++;
				result = parseExpression();
				exprNestingLevel--;
				insideAnExpr(result);
				break;

			case ')':
				throw new LispError("No opening parenthesis!");

			default:
				if (Character.isDigit(firstChar)) {
					result = parseNumber();
				} else if (Character.isLetter(firstChar) || isOperator(firstChar)) {
					result = (numberFollowsOperator(input, firstChar, index)) ? parseNumber() : parseIdentifier();
				} else {
					throw new LispError("Unexpected character: '" + firstChar + "'");
				}
				insideAnExpr(result);
			}

			return result;
		}

		private LispBoolean parseBoolean() throws LispError {
			Matcher matcher = Pattern.compile("#[tf]").matcher(input.substring(index));
			if (matcher.find()) {
				index += matcher.group().length();
				return LispBoolean.valueOf(matcher.group().equals("#t"));
			} else {
				throw new LispError("Invalid boolean representation");
			}
		}

		private LispExpression parseExpression() throws LispError {
			LispExpression expression = new LispExpression();
			consumeChar('(');
			skipWhiteSpace();
			int inputLen = input.length();

			while (index < inputLen && input.charAt(index) != ')') {
				LispItem item = parse();
				if (item != null)
					expression.append(item);
				skipWhiteSpace();
			}

			if (expression.toString().matches("\\([<>]\\)")) {
				throw new LispError("match --> " + expression.toString());
			}

			if (index == inputLen || input.charAt(index) != ')') {
				throw new LispError("Misformed Expression");
			}

			consumeChar(')');
			return expression;
		}

		private LispNumber parseNumber() throws NumberFormatException, LispError {
			StringBuilder numStr = new StringBuilder();
			int inputLen = input.length();
			char currentChar = input.charAt(index);

			while (String.valueOf(currentChar).matches("[\\d\\-\\.eE]")) {
				numStr.append(currentChar);
				index++;
				if (index >= inputLen) {
					break;
				}
				currentChar = input.charAt(index);
			}

			String lispNumStr = numStr.toString();
			if (lispNumStr.contains(".")) { // potential double!
				return new LispNumber(Double.valueOf(lispNumStr));
			}

			try {
				return new LispNumber(new BigInteger(lispNumStr));
			} catch (NumberFormatException nfe) {
				throw new LispError("Not a number", nfe);
			}
		}

		private LispIdentifier parseIdentifier() throws LispError {
			StringBuilder identifier = new StringBuilder();
			int inputLen = input.length();

			Matcher matcher = Pattern.compile("[^\\s()]+").matcher(input.substring(index));

			if (matcher.find()) {
				identifier.append(matcher.group());
				index += matcher.group().length();
			} else {
				throw new LispError("Unable to parse identifier");
			}

			String lispId = identifier.toString();
			String inputSubstring = input.substring(0, index - 1);

			if (index < inputLen && input.charAt(index) == ')' && !inputSubstring.contains("(")) {
				throw new LispError("No opening parenthesis!");
			}

			return new LispIdentifier(lispId);
		}

		private boolean isOperator(char op) {
			String operatorsRegex = "[+\\-*/<>=]";
			return String.valueOf(op).matches(operatorsRegex);
		}

		private void consumeChar(char expected) throws LispError {
			assert index < input.length() : "Unexpected end!";
			char currentChar = input.charAt(index);
			assert (currentChar == expected) : "Expected '" + expected + "', found '" + currentChar + "'";
			index++;
		}

		private void skipWhiteSpace() {
			while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
				index++;
			}
		}
	}

	@Override
	public LispItem evaluate(LispItem ex) throws LispError {
		if (ex instanceof LispBoolean) {
			return ex;
		} else if (ex instanceof LispIdentifier) {
			return evaluateIdentifier((LispIdentifier) ex);
		} else if (ex instanceof LispExpression) {
			return evaluateExpression((LispExpression) ex);
		} else {
			throw new LispError("Unsupported LispItem");
		}
	}

	private LispItem evaluateIdentifier(LispIdentifier identifier) throws LispError {
		throw new UnsupportedOperationException("Not implemented yet");
	}
	
	private LispItem evaluateExpression(LispExpression expression) throws LispError {
		if (expression.isEmpty()) {
			throw new LispError("Empty expression");
		}

		LispItem operatorItem = expression.values().car();

		if (operatorItem instanceof LispIdentifier) {
			String operator = ((LispIdentifier) operatorItem).toString();

			if (isBooleanOperator(operator)) {
				return evaluateBooleanExpression(expression);
			} else if (isComparisonOperator(operator)) {
				return evaluateComparisonExpression(expression);
			} else if (isArithmeticOperator(operator)) {
				return evaluateArithmeticExpression(expression);
			} else {
				throw new LispError("Unsupported operator: " + operator);
			}
		} else {
			throw new LispError("Invalid expression, operator expected");
		}
	}
	
	private LispItem evaluateComparisonExpression(LispExpression expression) throws LispError {
		if (expression.values().size() < 2) {
			throw new LispError("Invalid number of operands");
		}

		throw new UnsupportedOperationException("Not implemented yet");
	}
	
	/**
	 * Evaluate an expression of {@link LispExpression}
	 * 
	 * @param expression
	 * @return a {@link LispNumber}
	 * @throws LispError
	 */
	private LispItem evaluateArithmeticExpression(LispExpression expression) throws LispError {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	/* helpers */
	
	private boolean isBooleanOperator(String operator) {
		return operator.matches("and|or|not");
	}
	
	private boolean isComparisonOperator(String operator) {
	    return operator.matches("<|<=|>|>=|=");
	}

	private boolean isArithmeticOperator(String operator) {
		return operator.matches("[+\\-*/]");
	}

	/**
	 * Evaluate an expression of {@link LispExpression}
	 * 
	 * @param expression
	 * @return a {@link LispBoolean}
	 * @throws LispError
	 */
	private LispBoolean evaluateBooleanExpression(LispExpression expression) throws LispError {
		LispItem operatorItem = expression.values().car();
		String operator = ((LispIdentifier) operatorItem).toString();

		switch (operator) {
		case "and":
			return evaluateAnd(expression);
		case "or":
			return evaluateOr(expression);
		case "not":
			return evaluateNot(expression);
		default:
			throw new LispError("Unsupported boolean operator: " + operator);
		}
	}

	private LispBoolean evaluateAnd(LispExpression expression) throws LispError {
		boolean result = true;
		boolean operand;
		int i;
		for (i = 1; i < expression.values().size(); i++) {
			LispItem operandItem = expression.nth(i);
			if (!(operandItem instanceof LispBoolean)) {
				operand = evaluateBooleanExpression((LispExpression) operandItem).value();
			} else {
				operand = ((LispBoolean) operandItem).value();
			}
			result = result && operand;
		}

		return LispBoolean.valueOf(result);
	}

	private LispBoolean evaluateOr(LispExpression expression) throws LispError {
		boolean result = false;
		boolean operand;
		int i;
		for (i = 1; i < expression.values().size(); i++) {
			LispItem operandItem = expression.nth(i);
			if (!(operandItem instanceof LispBoolean)) {
				operand = evaluateBooleanExpression((LispExpression) operandItem).value();
			} else {
				operand = ((LispBoolean) operandItem).value();
			}
			result = result || operand;
		}

		return LispBoolean.valueOf(result);
	}

	private LispBoolean evaluateNot(LispExpression expression) throws LispError {
		if (expression.values().size() != 2) {
			throw new LispError("Invalid number of operands");
		}

		LispItem operandItem = expression.nth(1);
		boolean operand;
		if (!(operandItem instanceof LispBoolean)) {
			operand = evaluateBooleanExpression((LispExpression) operandItem).value();
		} else {
			operand = ((LispBoolean) operandItem).value();
		}

		return LispBoolean.valueOf(!operand);
	}

	private LispBoolean compareLispNumbers(LispExpression expression) throws LispError {
		if (expression.values().size() < 3) {
			throw new LispError("Invalid number of operands");
		}

		LispItem operandItem1 = expression.nth(1);
		LispItem operandItem2 = expression.nth(2);
		if (!(operandItem1 instanceof LispNumber) || !(operandItem2 instanceof LispNumber)) {
			throw new LispError("Operand of '=' must be a number");
		}
		Number operand1 = ((LispNumber) operandItem1).doubleValue();
		Number operand2 = ((LispNumber) operandItem2).doubleValue();

		return LispBoolean.valueOf(operand1 == operand2);
	}

	private LispBoolean compareLessThan(LispExpression expression) throws LispError {
		if (expression.values().size() != 3) {
			throw new LispError("Invalid number of operands");
		}

		LispItem operandItem1 = expression.nth(1);
		LispItem operandItem2 = expression.nth(2);
		if (!(operandItem1 instanceof LispNumber) || !(operandItem2 instanceof LispNumber)) {
			throw new LispError("Operands of < must be numbers");
		}
		double operand1 = ((LispNumber) operandItem1).doubleValue();
		double operand2 = ((LispNumber) operandItem2).doubleValue();

		return LispBoolean.valueOf(operand1 < operand2);
	}

	private LispBoolean compareLessThanOrEqual(LispExpression expression) throws LispError {
		if (expression.values().size() != 3) {
			throw new LispError("Invalid number of operands");
		}

		LispItem operandItem1 = expression.nth(1);
		LispItem operandItem2 = expression.nth(2);
		if (!(operandItem1 instanceof LispNumber) || !(operandItem2 instanceof LispNumber)) {
			throw new LispError("Operands of <= must be numbers");
		}
		double operand1 = ((LispNumber) operandItem1).doubleValue();
		double operand2 = ((LispNumber) operandItem2).doubleValue();

//		System.out.println("(op oprd oprd) --> " + operand1 + operand1);
		return LispBoolean.valueOf(operand1 <= operand2);
	}

	private LispBoolean compareGreaterThan(LispExpression expression) throws LispError {
		if (expression.values().size() != 3) {
			throw new LispError("Invalid number of operands");
		}

		LispItem operandItem1 = expression.nth(1);
		LispItem operandItem2 = expression.nth(2);
		if (!(operandItem1 instanceof LispNumber) || !(operandItem2 instanceof LispNumber)) {
			throw new LispError("Operands of > must be numbers");
		}
		double operand1 = ((LispNumber) operandItem1).doubleValue();
		double operand2 = ((LispNumber) operandItem2).doubleValue();

		return LispBoolean.valueOf(operand1 > operand2);
	}

	private LispBoolean compareGreaterThanOrEqual(LispExpression expression) throws LispError {
		if (expression.values().size() != 3) {
			throw new LispError("Invalid number of operands for");
		}

		LispItem operandItem1 = expression.nth(1);
		LispItem operandItem2 = expression.nth(2);
		if (!(operandItem1 instanceof LispNumber) || !(operandItem2 instanceof LispNumber)) {
			throw new LispError("Operands of >= must be numbers");
		}
		double operand1 = ((LispNumber) operandItem1).doubleValue();
		double operand2 = ((LispNumber) operandItem2).doubleValue();

		return LispBoolean.valueOf(operand1 >= operand2);
	}
}
