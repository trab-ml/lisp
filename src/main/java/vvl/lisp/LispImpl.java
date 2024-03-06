package vvl.lisp;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LispImpl implements Lisp {

	@Override
	public LispItem parse(String expr) throws LispError {
		LispParser parser = new LispParser(expr);
		LispItem parsed = parser.parse();

		while (!parser.isInputEmpty()) {
			parsed = parser.parse();
		}

		if (parser.getParsedLispItems().size() == 2) {
			throw new LispError("Unexpected data after parsing complete!");
		}

		return parsed;
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

			Matcher matcher = Pattern.compile("^\\-\\d+(\\.\\d+)?([eE]\\-?\\d+)? ").matcher(input.substring(index));
			if (matcher.find()) {
				throw new LispError("- should be a lisp operator");
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

			String expr = expression.toString();
			if (expr.matches("\\([<>]\\)") || expr.matches("(<)|(<=)|(>)|(>=)|(=)")) {
				throw new LispError("Invalid number of operands");
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
		if (identifier.equals("nil")) {
			return new LispExpression();
		}
		return identifier;
	}

	private LispItem evaluateExpression(LispExpression expression) throws LispError {
		int expressionSize = expression.values().size();
		if (expression.isEmpty()) {
			return expression;
		} else if (expressionSize == 1) {
			String carVal = expression.toString();
			if (carVal.equals("(nil)")) {
				throw new LispError("nil is not a valid operator");
			} else if (carVal.equals("(+)")) {
				return new LispNumber(0);
			} else if (carVal.equals("(*)")) {
				return new LispNumber(1);
			} else if (carVal.equals("(/)")) {
				throw new LispError("Invalid number of operands");
			}
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
			} else if (operator.equals("cons")) {
				if (expressionSize < 3) {
					throw new LispError("Invalid number of operands");
				}
				return evaluateConsExpression(expression);
			} else if (operator.equals("quote")) {
				if (expressionSize < 2) {
					throw new LispError("Invalid number of operands");
				}
				return evaluateQuoteExpression(expression);
			} else if (operator.equals("list")) {
				return evaluateListExpression(expression);
			} else if (operator.equals("if")) {
				if (expressionSize != 4) {
					throw new LispError("Invalid number of operands");
				}
				return evaluateIfExpression(expression);
			} else if (operator.equals("car")) {
				return evaluateCarExpression(expression);
			} else if (operator.equals("cdr")) {
				return evaluateCdrExpression(expression);
			} else {
				throw new LispError("Unsupported operator: " + operator);
			}
		} else {
			throw new LispError("Invalid expression, operator expected");
		}
	}

	private LispBoolean evaluateComparisonExpression(LispExpression expression) throws LispError {
		LispItem operatorItem = expression.values().car();
		String operator = ((LispIdentifier) operatorItem).toString();
		if (expression.values().size() < 2) {
			throw new LispError("Invalid number of operands");
		}

		switch (operator) {
		case "=":
			return compareEqualsTo(expression);
		case "<=":
			return compareLessThanOrEqual(expression);
		case "<":
			return compareLessThan(expression);
		case ">=":
			return compareGreaterThanOrEqual(expression);
		case ">":
			return compareGreaterThan(expression);
		default:
			throw new LispError("Unsupported comparason operator " + operator);
		}
	}

	/**
	 * Evaluate an expression of {@link LispExpression}
	 * 
	 * @param expression
	 * @return a {@link LispNumber}
	 * @throws LispError
	 */
	private LispNumber evaluateArithmeticExpression(LispExpression expression) throws LispError {
		Iterator<LispItem> it = expression.values().iterator();
		String operator = ((LispIdentifier) it.next()).toString();
		LispNumber result = getNumericValue(it.next());
		int exprLenght = expression.values().size();
		if (exprLenght == 2) {
			if (operator.equals("-")) {
				return result.multiply(new LispNumber(-1));
			}
		}
		if (exprLenght != 3 && (operator.equals("-") || operator.equals("/"))) {
			throw new LispError("Invalid number of operands");
		}

		LispNumber operand;
		while (it.hasNext()) {
			operand = getNumericValue(it.next());
			switch (operator) {
			case "+":
				result = result.add(operand);
				break;
			case "-":
				result = result.subtract(operand);
				break;
			case "*":
				result = result.multiply(operand);
				break;
			case "/":
				if (operand.toString().equals("0")) {
					throw new LispError("Division by zero");
				}
				result = result.divide(operand);
				break;
			default:
				throw new LispError("Unsupported arithmetic operator: " + operator);
			}
		}

		return result;
	}

	private LispExpression extractConsExpressionItemsValuesToResult(LispExpression expression, LispExpression result) throws LispError {
		result.append(expression.values().car());
		if (expression.values().size() == 1) {
			return result;
		}
		return extractConsExpressionItemsValuesToResult((LispExpression) expression.values().cdr(), result);
	}
	
	private LispExpression evaluateConsExpression(LispExpression expression) throws LispError {
		Iterator<LispItem> it = expression.values().iterator();
		LispItem operator = it.next(); // 'cons' operator
		LispItem item1 = getConsExpressionItemValue(it.next());
		LispItem item2 = getConsExpressionItemValue(it.next());
		LispExpression result = new LispExpression();

		if (item2 instanceof LispExpression) {
			LispExpression resExprItem = (LispExpression) item2;
			if (resExprItem.isEmpty()) {
				result.append(item1);
			} else {
//			} else if (resExprItem.values().size() == 1) {
				result.append(item1);
//				result.append(resExprItem);
				
				result = extractConsExpressionItemsValuesToResult(resExprItem, result);
			}
		} else {
			result.append(item1);
			result.append(new LispIdentifier("."));
			result.append(item2);
		}
		return result;
	}

	private LispExpression evaluateQuoteExpression(LispExpression expression) throws LispError {
		return (LispExpression) expression.values().cdr();
	}

	private LispItem evaluateIfExpression(LispExpression expression) throws LispError {
		Iterator<LispItem> it = expression.values().iterator();
		it.next();
//		boolean operand = getIfExpressionOperandValue(it.next()).value();
		boolean operand = getBooleanValue(it.next()).value();
		LispItem result = it.next(); // operand1
		if (!operand) {
			result = it.next(); // operand2
		}

		if (result instanceof LispExpression) {
			result = evaluateExpression((LispExpression) result);
		}
		return result;
	}

	private LispItem evaluateListExpression(LispExpression expression) throws LispError {
		Iterator<LispItem> it = expression.values().iterator();
		it.next();
		LispExpression result = new LispExpression();
		LispItem nextElt;
		while (it.hasNext()) {
			nextElt = it.next();
			if (nextElt instanceof LispExpression) {
				nextElt = evaluateExpression((LispExpression) nextElt);
			}
			result.append(nextElt);
		}
		return result;
	}

	private LispItem evaluateCarExpression(LispExpression expression) throws LispError {
		LispExpression result = (LispExpression) evaluateExpression((LispExpression) expression.values().cdr());
//		if (result.values().size() == 2) {
//			throw new LispError("Not a Cons");
//		}
		return ((LispExpression) result.values().car());
//		return expression;
	}

	private LispItem evaluateCdrExpression(LispExpression expression) throws LispError {
		LispExpression result = (LispExpression) evaluateExpression((LispExpression) expression.values().cdr());
//		if (result.values().size() == 2) {
//			throw new LispError("Not a Cons");
//		}
		return ((LispExpression) result.values().cdr());
//		return expression;
	}

//	throw new UnsupportedOperationException("Not implemented yet");

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

	private LispNumber getNumericValue(LispItem item) throws LispError {
		if (!(item instanceof LispNumber)) {
			return (LispNumber) evaluateArithmeticExpression((LispExpression) item);
		}
		return (LispNumber) item;
	}

	private LispBoolean getBooleanValue(LispItem item) throws LispError {
		if (!(item instanceof LispBoolean)) {
			LispItem elt = evaluateExpression((LispExpression) item);
			if (!(elt instanceof LispBoolean)) {
				throw new LispError("Not a Boolean");
			}
			return (LispBoolean) elt;

//			return (LispBoolean) evaluateBooleanExpression((LispExpression) item);
		}
		return (LispBoolean) item;
	}

	private LispItem getConsExpressionItemValue(LispItem item) throws LispError {
		if (item instanceof LispExpression) {
			return (LispExpression) evaluateExpression((LispExpression) item);
		} else if (item instanceof LispIdentifier && ((LispIdentifier) item).equals("nil")) {
			return new LispExpression();
		}
		return item;
	}

//	private LispBoolean getIfExpressionOperandValue(LispItem item) throws LispError {
//		if (item instanceof LispBoolean) {
//			return (LispBoolean) item;
//		}
//		
//		LispItem result = evaluateComparisonExpression((LispExpression) item);
//		if (!(item instanceof LispBoolean)) {
//			throw new LispError("Not a Boolean");
//		}
//		return (LispBoolean) result;
//	}

	/**
	 * Evaluate an expression {@link LispExpression}
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
//			return evaluateExpression(expression);
			throw new LispError("Unsupported boolean operator: " + operator);
		}
	}

	private LispBoolean evaluateAnd(LispExpression expression) throws LispError {
		boolean result = true;
		boolean operand;
		Iterator<LispItem> it = expression.values().iterator();
		it.next(); // skip the first element (the operator)
		while (it.hasNext()) {
			operand = getBooleanValue(it.next()).value();
			result = result && operand;

			if (!result) {
				break;
			}
		}

		return LispBoolean.valueOf(result);
	}

	private LispBoolean evaluateOr(LispExpression expression) throws LispError {
		boolean result = false;
		boolean operand;
		Iterator<LispItem> it = expression.values().iterator();
		it.next();
		while (it.hasNext()) {
			operand = getBooleanValue(it.next()).value();
			result = result || operand;

			if (result) {
				break;
			}
		}
		return LispBoolean.valueOf(result);
	}

	private LispBoolean evaluateNot(LispExpression expression) throws LispError {
		if (expression.values().size() != 2) {
			throw new LispError("Invalid number of operands");
		}
		LispItem operandItem = expression.nth(1);
		boolean operand = getBooleanValue(operandItem).value();
		return LispBoolean.valueOf(!operand);
	}

	private LispBoolean compareEqualsTo(LispExpression expression) throws LispError {
		Iterator<LispItem> it = expression.values().iterator();
		it.next();
		LispNumber firstOperand = getNumericValue(it.next());
		LispNumber operand;
		while (it.hasNext()) {
			operand = getNumericValue(it.next());
			if (firstOperand.compareTo(operand) != 0) {
				return LispBoolean.valueOf(false);
			}
		}
		return LispBoolean.valueOf(true);
	}

	private LispBoolean compareLessThan(LispExpression expression) throws LispError {
		Iterator<LispItem> it = expression.values().iterator();
		it.next();
		LispNumber prev = getNumericValue(it.next());
		LispNumber curr;
		while (it.hasNext()) {
			curr = getNumericValue(it.next());
			if (prev.compareTo(curr) != -1) {
				return LispBoolean.valueOf(false);
			}
			prev = curr;
		}
		return LispBoolean.valueOf(true);
	}

	private LispBoolean compareLessThanOrEqual(LispExpression expression) throws LispError {
		Iterator<LispItem> it = expression.values().iterator();
		it.next();
		LispNumber prev = getNumericValue(it.next());
		LispNumber curr;
		while (it.hasNext()) {
			curr = getNumericValue(it.next());
			if (prev.compareTo(curr) > 0) {
				return LispBoolean.valueOf(false);
			}
			prev = curr;
		}
		return LispBoolean.valueOf(true);
	}

	private LispBoolean compareGreaterThan(LispExpression expression) throws LispError {
		Iterator<LispItem> it = expression.values().iterator();
		it.next();
		LispNumber prev = getNumericValue(it.next());
		LispNumber curr;
		while (it.hasNext()) {
			curr = getNumericValue(it.next());
			if (prev.compareTo(curr) <= 0) {
				return LispBoolean.valueOf(false);
			}
			prev = curr;
		}
		return LispBoolean.valueOf(true);
	}

	private LispBoolean compareGreaterThanOrEqual(LispExpression expression) throws LispError {
		Iterator<LispItem> it = expression.values().iterator();
		it.next();
		LispNumber prev = getNumericValue(it.next());
		LispNumber curr;
		while (it.hasNext()) {
			curr = getNumericValue(it.next());
			if (prev.compareTo(curr) < 0) {
				return LispBoolean.valueOf(false);
			}
			prev = curr;
		}
		return LispBoolean.valueOf(true);
	}
}
