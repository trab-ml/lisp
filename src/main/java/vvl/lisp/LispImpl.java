package vvl.lisp;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;

public class LispImpl implements Lisp {

	@Override
	public LispItem parse(String expr) throws LispError {
		expr = expr.trim();
		LispParser parser = new LispParser(expr);
		LispItem parsed = parser.parse();
		ArrayList<LispItem> parsedLispItems = parser.getParsedLispItems();

//		expr has been entirely parsed (not only the first LispItem)?		
		int index = parser.getIndex();
		int exprLen = expr.length();
		while (index < exprLen) {
			parsed = parser.parse();
			index = parser.getIndex();
		}

		if (parsedLispItems.size() == 2) {
			Iterator<LispItem> iterator = parsedLispItems.iterator();
			LispItem firstItem = iterator.next();
			LispItem secondItem = iterator.next();
			if ((firstItem instanceof LispExpression && secondItem instanceof LispIdentifier)
					|| (firstItem instanceof LispIdentifier && secondItem instanceof LispIdentifier)
					|| (firstItem instanceof LispExpression && secondItem instanceof LispExpression)) {
				throw new LispError("Remaining data cannot be ignored!");
			}
		}

		return parsed;

	}

	@Override
	public LispItem evaluate(LispItem ex) {
		return null;
	}

	private static class LispParser {

		private final String input;
		private int index = 0;
		private ArrayList<LispItem> lispItemsList = new ArrayList<LispItem>();
		private Boolean idIsInsideAnExpr = false;
		private int exprNestingLevel = 0;

		public LispParser(String input) {
			this.input = input.trim();
		}

		public ArrayList<LispItem> getParsedLispItems() {
			return lispItemsList;
		}

		public int getIndex() {
			return index;
		}
		
		private void insideAnExpr(LispItem it) {
			if (!idIsInsideAnExpr)
				lispItemsList.add(it);
		}

		private LispItem parse() throws LispError {
			skipWhiteSpace();
			if (index >= input.length()) {
				throw new LispError("Unauthorized expression --> `" + input.substring(index, input.length()) + "`");
			}

			char firstChar = input.charAt(index);
			LispItem result;

			switch (firstChar) {
			case '#':
				result = parseBoolean();
				if (!idIsInsideAnExpr)
					lispItemsList.add(result);
				break;

			case '(':
				exprNestingLevel++;
				idIsInsideAnExpr = true;
				result = parseExpression();
				exprNestingLevel--;
				idIsInsideAnExpr = false;
				if (exprNestingLevel == 0) {
					lispItemsList.add(result);
				}
				break;

			case ')':
				throw new LispError("No opening parenthesis!");

			default:
				if (Character.isDigit(firstChar)) {
					result = parseNumber();
				} else if (Character.isLetter(firstChar) || isOperator(firstChar)) {
					if (firstChar == '-' && index + 1 < input.length()
							&& (Character.isDigit(input.charAt(index + 1)) || input.charAt(index + 1) == '.')) {
						result = parseNumber();
					} else {
						result = parseIdentifier();
					}
				} else {
					throw new LispError("Unexpected character: '" + firstChar + "'");
				}
				insideAnExpr(result);
			}

			return result;
		}

		private LispBoolean parseBoolean() throws LispError {
			StringBuilder boolStr = new StringBuilder();
			boolStr.append(input.charAt(index++)); // Consume '#'

			while (index < input.length() && !Character.isWhitespace(input.charAt(index))
					&& input.charAt(index) != ')') {
				boolStr.append(input.charAt(index++)); // Consume a char
			}

			if (boolStr.toString().equals("#t")) {
				return LispBoolean.TRUE;
			} else if (boolStr.toString().equals("#f")) {
				return LispBoolean.FALSE;
			} else {
				throw new LispError("Invalid boolean representation: " + boolStr.toString());
			}
		}

		private LispExpression parseExpression() throws LispError {
			LispExpression expression = new LispExpression();
			consumeChar('(');
			skipWhiteSpace();

			while (index < input.length() && input.charAt(index) != ')') {
				LispItem item = parse();
				if (item != null)
					expression.append(item);
				skipWhiteSpace();
			}

			if (index == input.length() || input.charAt(index) != ')') {
				throw new LispError("No ending parenthesis!");
			}
			consumeChar(')');
			skipWhiteSpace();
			return expression;
		}

		private LispNumber parseNumber() throws NumberFormatException, LispError {
			StringBuilder numStr = new StringBuilder(); // consume the first char of a potential number
			int inputLen = input.length();
			char currentChar = input.charAt(index);

			while (Character.isDigit(currentChar) || currentChar == '-' || currentChar == '.' || currentChar == 'e'
					|| currentChar == 'E') {
				numStr.append(currentChar);
				index++;
				if (index >= inputLen) {
					break;
				}
				currentChar = input.charAt(index);
			}

			String lispNumStr = numStr.toString();
			if (lispNumStr.indexOf('.') != -1) { // potential double!
				return new LispNumber(Double.valueOf(lispNumStr));
			}

			LispNumber result;
			try {
				result = new LispNumber(new BigInteger(lispNumStr));
			} catch (NumberFormatException nfe) {
				throw new LispError("Deal with smtg which is not a number", nfe);
			}
			return result;
		}

		private LispIdentifier parseIdentifier() throws LispError {
			StringBuilder identifier = new StringBuilder();
			int inputLen = input.length();
			while (index < inputLen && input.charAt(index) != ')' && !Character.isWhitespace(input.charAt(index))) {
				identifier.append(input.charAt(index));
				index++;
			}

			String lispId = identifier.toString();
			String inputSubstring = input.substring(0, index - 1);
			if (index < inputLen && input.charAt(index) == ')' && (inputSubstring.indexOf('(') == -1)) {
				throw new LispError("No opening parenthesis!");
			}

			return new LispIdentifier(lispId);
		}

		private boolean isOperator(char op) {
			return op == '+' || op == '-' || op == '*' || op == '/' || op == '>';
		}

		private void consumeChar(char expected) throws LispError {
			if (index < input.length()) {
				char currentChar = input.charAt(index);
				if (currentChar == expected) {
					index++;
				} else {
					throw new LispError("Expected '" + expected + "', found '" + currentChar + "'");
				}
			}
		}

		private void skipWhiteSpace() {
			while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
				index++;
			}
		}
	}
}
