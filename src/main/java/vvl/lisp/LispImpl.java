package vvl.lisp;

import java.math.BigInteger;

public class LispImpl implements Lisp {

	@Override
	public LispItem parse(String expr) throws LispError {
		LispParser parser = new LispParser(expr);
		return parser.parse();
	}

	@Override
	public LispItem evaluate(LispItem ex) {
		return null;
	}

	private static class LispParser {

		private final String input;
		private int index;

		public LispParser(String input) {
			this.input = input.trim();
			this.index = 0;
		}

		private LispItem parse() throws LispError {
			skipWhiteSpace();
			if (index >= input.length()) {
				throw new LispError("Empty expression");
			}

			char firstChar = input.charAt(index);

			if (firstChar == '#') {
				return parseBoolean();
			} else if (firstChar == '(') {
				LispExpression myList = parseExpression();
				return myList;
			} else if (Character.isDigit(firstChar)) {
				return parseNumber();
			} else if (Character.isLetter(firstChar) || isOperator(firstChar)) {
				if ((firstChar == '-' && index + 1 < input.length() && 
			            (Character.isDigit(input.charAt(index + 1)) || input.charAt(index + 1) == '.'))) {
			        return parseNumber();
			    } else {
			        return parseIdentifier();
			    }
			} else {
				throw new LispError("Unexpected character: '" + firstChar + "'");
			}
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
				expression.append(item);
				skipWhiteSpace();
			}
			consumeChar(')');
			return expression;
		}

		private LispNumber parseNumber() throws NumberFormatException, LispError {
			StringBuilder numStr = new StringBuilder(); // consume the first char of a potential number
			int inputLen = input.length();
			char currentChar = input.charAt(index);

			while (Character.isDigit(currentChar) || currentChar == '-' || currentChar == '.' || currentChar == 'E') {
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

		private LispIdentifier parseIdentifier() {
			StringBuilder identifier = new StringBuilder();
			while (index < input.length() && input.charAt(index) != ')' && !Character.isWhitespace(input.charAt(index))) {
				identifier.append(input.charAt(index));
				index++;
			}
			LispIdentifier id = new LispIdentifier(identifier.toString());
			return id;
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
