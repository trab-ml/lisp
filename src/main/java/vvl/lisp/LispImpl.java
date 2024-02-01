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
            throw new LispError("Error while parsing expression: " + e.getMessage(), e);
        }
	}
	
	@Override
	public LispItem evaluate(LispItem ex) throws LispError {
        if (ex instanceof LispExpression) {
            LispExpression expression = (LispExpression) ex;
            if (expression.values().isEmpty()) {
                throw new LispError("Empty expression");
            }

            LispItem firstItem = expression.values().car();

            if (firstItem instanceof LispIdentifier) {
                String operator = ((LispIdentifier) firstItem).toString();

                if (isArithmeticOperator(operator)) {
                    return evaluateArithmeticExpression(expression);
                }
            }

            throw new LispError("Unsupported operator: " + firstItem);
        }

        return ex;
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
			int inputIndex = index;

			while (index < inputLen && input.charAt(index) != ')') {
				LispItem item = parse();
				if (item != null)
					expression.append(item);
				skipWhiteSpace();
			}
			
			if (expression.toString().matches("\\([<>]\\)")) {
				throw new LispError("match --> " + expression.toString());
			}
			
			// empty or misformed expr
//			if (index == inputIndex ) {}
			
			if (index == inputLen 
					|| input.charAt(index) != ')') {
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
            String operatorsRegex = "[+\\-*/<>]";
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
	
	/*
	 * Helpers to evaluate a parse LispItem
	 */
	
	private boolean isArithmeticOperator(String operator) {
        return operator.matches("[+\\-*/]");
    }

	/**
	 * 
	 * @param expression
	 * @return
	 * @throws LispError
	 */
	private LispNumber evaluateArithmeticExpression(LispExpression expression) throws LispError {
		return null;
	}
}
