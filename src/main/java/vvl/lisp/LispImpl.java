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
            if (input.isEmpty()) {
                throw new LispError("Empty expression");
            }

            char firstChar = input.charAt(index);
            
            if (firstChar == '#') {
            	return parseBoolean();
            } else if (firstChar == '(') {
            	LispExpression myList = parseExpression();
//            	System.out.println("--> " + myList.toString());
            	return myList;
            } else if (Character.isLetter(firstChar)) {
            	return parseIdentifier();
            } else if (Character.isDigit(firstChar) || firstChar == '-') {
                return parseNumber();
            } else {
                throw new LispError("Unexpected character: " + firstChar);
            }
        }

        private LispBoolean parseBoolean() throws LispError {
            StringBuilder boolStr = new StringBuilder();
            boolStr.append(input.charAt(index++)); // Consume '#'
            
            while (index < input.length() 
            		&& !Character.isWhitespace(input.charAt(index))
            		&& input.charAt(index)!=')'
            ) {
            	boolStr.append(input.charAt(index++)); // Consume a char
            }
            
            if (boolStr.toString().equals("#t")) {
//            	System.out.println("--> #t");
                return LispBoolean.TRUE;
            } else if (boolStr.toString().equals("#f")) {
//            	System.out.println("--> #f");
                return LispBoolean.FALSE;
            } else {
                throw new LispError("Invalid boolean representation: " + boolStr.toString());
            }
        }

	private LispExpression parseExpression() throws LispError {
		LispExpression expression = new LispExpression();
		consumeChar('(');
//		System.out.println("^expr");
		skipWhiteSpace();

		while (input.charAt(index) != ')') {
			LispItem item = parse();
			expression.append(item);
			skipWhiteSpace();
		}

		consumeChar(')');
//		System.out.println("expr$");
		return expression;
	}

	private LispNumber parseNumber() {
		StringBuilder numStr = new StringBuilder();
		char currentChar = input.charAt(index);

		while (Character.isDigit(currentChar) || currentChar == '-' || currentChar == '.') {
			numStr.append(currentChar);
			index++;

			if (index >= input.length()) {
				break;
			}

			currentChar = input.charAt(index);
		}

		return new LispNumber(new BigInteger(numStr.toString()));
	}

	private LispIdentifier parseIdentifier() {
		StringBuilder identifier = new StringBuilder();

		while (index < input.length() && !Character.isWhitespace(input.charAt(index))) {
			identifier.append(input.charAt(index));
			index++;
		}

//        System.out.println("ID");
		return new LispIdentifier(identifier.toString());
	}

	private void consumeChar(char expected) throws LispError {
		skipWhiteSpace();
		char currentChar = input.charAt(index);

		if (currentChar == expected) {
			index++;
		} else {
			throw new LispError("Expected '" + expected + "', found '" + currentChar + "'");
		}
	}

	private void skipWhiteSpace() {
		while (index < input.length() && Character.isWhitespace(input.charAt(index))) {
			index++;
		}
	}
}}
