package vvl.lisp;

import java.math.BigInteger;
import java.util.ArrayList;

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
            throw new LispError("Error while parsing expression: " + e.getMessage(), e);
        }

	}

	@Override
	public LispItem evaluate(LispItem ex) {
		return null;
	}

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
			return c == '-' && i + 1 < in.length() && (Character.isDigit(in.charAt(i + 1)) || in.charAt(i + 1) == '.');
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
			int inputLen = input.length();

			while (index < inputLen && input.charAt(index) != ')') {
				LispItem item = parse();
				if (item != null)
					expression.append(item);
				skipWhiteSpace();
			}

			if (index == inputLen || input.charAt(index) != ')') {
				throw new LispError("No ending parenthesis!");
			}
			consumeChar(')');
			return expression;
		}

		private LispNumber parseNumber() throws NumberFormatException, LispError {
			StringBuilder numStr = new StringBuilder();
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

			try {
				return new LispNumber(new BigInteger(lispNumStr));
			} catch (NumberFormatException nfe) {
				throw new LispError("Not a number", nfe);
			}
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
}
