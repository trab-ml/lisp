package vvl.lisp;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LispImpl implements Lisp {

	private Map<String, LispItem> globalVar = new HashMap<String, LispItem>();
	private Map<String, LispExpression> globalLambdaFct = new HashMap<String, LispExpression>();
	private Map<String, Map<String, LispItem>> globalLambdaFctContext = new HashMap<String, Map<String, LispItem>>();
	private Set<String> LISP_KEYWORDS = new HashSet<String>(Arrays.asList("or", "not", "and", "lambda", "define",
			"set!", "cons", "#t", "#f", "nil", "car", "cdr", "list"));
	private String LAMBDA_FUNCTION_TEMPORARY_NAME = "TEMPORARY_MAP_EXPRESSION_LAMBDA_FUNCTION_NAME";

	private boolean isLambdaFunction = false;
	private String parentLambdaFunctionContext = "";
	int globalCpt = 1;

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
		String id = identifier.toString();
		if (id.equals("nil")) {
			return new LispExpression();
		} else if (globalVar.containsKey(id)) {
			return globalVar.get(id);
		} else if (globalLambdaFct.containsKey(id)) {
			return globalLambdaFct.get(id);
		}
		throw new LispError(id + " is undefined");
	}

	/**
	 * Evaluate a parsed expression {@link LispExpression}
	 * 
	 * @param a {@link LispExpression}
	 * @return a {@link LispItem}
	 * @throws LispError
	 */
	private LispItem evaluateExpression(LispExpression expression) throws LispError {
		int expressionSize = expression.values().size();
		if (expression.isEmpty()) {
			return expression;
		} else if (expressionSize == 1) {
			String carVal = expression.toString();
			switch (carVal) {
			case "(nil)":
				throw new LispError("nil is not a valid operator");
			case "(+)":
				return new LispNumber(0);
			case "(*)":
				return new LispNumber(1);
			case "(/)":
			case "(-)":
				throw new LispError("Invalid number of operands");
			}
		}

		LispItem operatorItem = expression.values().car();
		if (operatorItem instanceof LispIdentifier) {
			LispItem evaluatedExpr;
			String operator = ((LispIdentifier) operatorItem).toString();
			if (isBooleanOperator(operator)) {
				evaluatedExpr = evaluateBooleanExpression(expression);
			} else if (isComparisonOperator(operator)) {
				evaluatedExpr = evaluateComparisonExpression(expression);
			} else if (isArithmeticOperator(operator)) {
				evaluatedExpr = evaluateArithmeticExpression(expression);
			} else if (operator.equals("cons")) {
				if (expressionSize < 3) {
					throw new LispError("Invalid number of operands");
				}
				evaluatedExpr = evaluateConsExpression(expression);
			} else if (operator.equals("quote")) {
				if (expressionSize != 2) {
					throw new LispError("Invalid number of operands");
				}
				evaluatedExpr = evaluateQuoteExpression(expression);
			} else if (operator.equals("list")) {
				evaluatedExpr = evaluateListExpression(expression);
			} else if (operator.equals("if")) {
				if (expressionSize != 4) {
					throw new LispError("Invalid number of operands");
				}
				evaluatedExpr = evaluateIfExpression(expression);
			} else if (operator.equals("car")) {
				evaluatedExpr = evaluateCarExpression(expression);
			} else if (operator.equals("cdr")) {
				evaluatedExpr = evaluateCdrExpression(expression);
			} else if (operator.equals("define") && expressionSize == 3) {
				evaluatedExpr = evaluateDefineExpression(expression);
			} else if (operator.equals("set!") && expressionSize == 3) {
				evaluatedExpr = evaluateSetExpression(expression);
			} else if (operator.equals("map") && expressionSize == 3) {
				isLambdaFunction = true;
				parentLambdaFunctionContext = LAMBDA_FUNCTION_TEMPORARY_NAME;
				evaluatedExpr = evaluateMapExpression(expression);
				isLambdaFunction = false;
				parentLambdaFunctionContext = "";
			} else if (globalLambdaFct.containsKey(operator)) {
				isLambdaFunction = true;
				parentLambdaFunctionContext = operator;
				// System.out.println("expression -> " + expression);
				evaluatedExpr = evaluateLambdaExpression(expression);
				isLambdaFunction = false;
				parentLambdaFunctionContext = "";
			} else if (isLambdaFunction
					&& globalLambdaFctContext.get(parentLambdaFunctionContext).containsKey(operator)) {
				evaluatedExpr = evaluateLambdaExpression(expression);
			} else {
//				System.out.println("parentLambdaFunctionContext --> " + parentLambdaFunctionContext);
				throw new LispError(operator + " is undefined");
			}
			return evaluatedExpr;
		} else if (operatorItem instanceof LispExpression) {
//			System.out.println("Potential imbricated fct! --> " + operatorItem);
			throw new UnsupportedOperationException("Unimplemented yet!");
		} else {
//			System.out.println(parentLambdaFunctionContext);
			throw new LispError("Invalid expression --> " + expression.toString());
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

	private LispExpression evaluateConsExpression(LispExpression expression) throws LispError {
		Iterator<LispItem> it = expression.values().iterator();
		it.next(); // 'cons' operator
		LispItem item1 = getConsExpressionItemValue(it.next());
		LispItem item2 = getConsExpressionItemValue(it.next());
		LispExpression result = new LispExpression();
		if (item2 instanceof LispExpression) {
			LispExpression resExprItem = (LispExpression) item2;
			result.append(item1);

			if (!resExprItem.isEmpty()) {
				Iterator<LispItem> item2It = resExprItem.values().iterator();
				while (item2It.hasNext()) {
					result.append(item2It.next());
				}
				return result;
			}
		} else {
			result.append(item1);
			result.append(new LispIdentifier("."));
			result.append(item2);
		}
		return result;
	}

	private LispItem evaluateQuoteExpression(LispExpression expression) throws LispError {
		Iterator<LispItem> it = expression.values().iterator();
		it.next();
		return it.next();
	}

	private LispItem evaluateIfExpression(LispExpression expression) throws LispError {
		Iterator<LispItem> it = expression.values().iterator();
		it.next();
		boolean operand = getBooleanValue(it.next()).value();
		LispItem result = it.next();
		if (!operand) {
			result = it.next();
		}
		if (result instanceof LispExpression) {
			result = evaluateExpression((LispExpression) result);
		}
		if (result instanceof LispIdentifier) {
			String id = ((LispIdentifier) result).toString();
			if (globalVar.containsKey(id)) {
				return globalVar.get(id);
			}
		}
		return result;
	}

	private LispExpression evaluateListExpression(LispExpression expression) throws LispError {
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
		if (expression.values().size() != 2) {
			throw new LispError("Invalid number of operands");
		}

		Iterator<LispItem> it = expression.values().iterator();
		it.next();
		LispExpression remainingExpr = (LispExpression) it.next();
		if (!remainingExpr.isEmpty()) {
			return processCarExpression(remainingExpr).values().car();
		}
		return new LispExpression();
	}

	private LispItem evaluateCdrExpression(LispExpression expression) throws LispError {
		if (expression.values().size() != 2) {
			throw new LispError("Invalid number of operands");
		}

		Iterator<LispItem> it = expression.values().iterator();
		it.next();
		if (it.hasNext()) {
			LispExpression remainingExpr = (LispExpression) it.next();
			if (!remainingExpr.isEmpty()) {
				it = remainingExpr.values().iterator();
				String operator = ((LispIdentifier) it.next()).toString();
				if (operator.equals("cons")) {
					it.next();
					LispItem nextElt = it.next();
					if (nextElt instanceof LispNumber
							|| (nextElt instanceof LispExpression && ((LispExpression) nextElt).isEmpty()))
						return nextElt;
					return evaluateConsExpression((LispExpression) nextElt);
				} else if (operator.equals("list")) {
					it = evaluateListExpression(remainingExpr).values().iterator();
				} else {
					throw new LispError("Not a Cons");
				}
				LispItem nextItem = it.next();
				if (nextItem instanceof LispExpression) {
					return nextItem;
				}
				LispExpression result = new LispExpression();
				while (it.hasNext()) {
					nextItem = it.next();
					if (!(nextItem instanceof LispIdentifier)) {
						result.append(nextItem);
					}
				}
				return result;
			}
		}
		return new LispExpression();
	}

	private LispItem evaluateDefineExpression(LispExpression expression) throws LispError {
		Iterator<LispItem> it = expression.values().iterator();
		it.next();
		LispItem nextItem = it.next();
		String varName = nextItem.toString();

		if (!(nextItem instanceof LispIdentifier)) {
			throw new LispError(varName + " is not a valid identifier");
		}
		if (!varName.matches("[a-zA-Z]+\\w*") || isKeyword(varName) || globalVar.containsKey(varName)) {
			throw new LispError(varName + " is not a valid identifier");
		}
		nextItem = it.next();
		if (nextItem instanceof LispExpression) {
			LispExpression expr = (LispExpression) nextItem;
			if (isValidLambdaExpression(expr)) {
				globalLambdaFct.put(varName, expr);
				return new LispIdentifier(extractLambdaExpression(expr)); // to exclude parenthesis, can't return String
																			// in this fct
			} else {
				throw new LispError("Invalid number of operands");
			}
//			nextItem = evaluateExpression(expr);
		}
		globalVar.put(varName, nextItem);
		return nextItem;
	}

	private LispItem evaluateSetExpression(LispExpression expression) throws LispError {
		Iterator<LispItem> it = expression.values().iterator();
		it.next();
		LispItem nextItem = it.next();
		String varName = nextItem.toString();

		if (!(nextItem instanceof LispIdentifier)) {
			throw new LispError(varName + " is not a valid identifier");
		}
		if (!varName.matches("[a-zA-Z]+\\w*") || isKeyword(varName)) {
			throw new LispError(varName + " is not a valid identifier");
		}
		if (globalVar.containsKey(varName)) {
			nextItem = it.next();
			if (nextItem instanceof LispExpression) {
				nextItem = evaluateExpression((LispExpression) nextItem);
			}
			globalVar.put(varName, nextItem);
			return nextItem;
		}
		throw new LispError(varName + " is undefined");
	}

//	NOT USE GLOBAL VAR IN THIS-->
	private LispItem evaluateLambdaExpression(LispExpression expression) throws LispError {
		// System.out.println("\n" + globalCpt + ") expression --> " + expression);
		globalCpt++;
		Iterator<LispItem> givenExprIt = expression.values().iterator();
		String fctName = ((LispIdentifier) givenExprIt.next()).toString();
		
		if (!globalLambdaFct.containsKey(fctName)) {
			// Shared context or not?
			// > (define twice (lambda (x) (* 2 x)))
			// > (define strange (lambda (f x) (f (f x))))
			// > (strange twice 10)
			// To make such as imbricated expression work, twice should have access to strange function context.
			
			// Comment part1) and uncomment part2) to observe:

			// part1)
			// Unknown parameter in child context

			// System.out.println("Unknown fctName --> " + fctName);

			// part2)
			// ASK parent / ancestor.
			fctName = globalLambdaFctContext.get(parentLambdaFunctionContext).get(fctName).toString();

			// part3)
			// By doing so above example (with twice and strange work), but how to deal with functions such as fibonacci sequences?
			// That's break this logic! 
			// ex:
			// (define fib (lambda (n) (if (< n 2) 1 (+ (fib (- n 1)) (fib (- n 2))))))
			// > (fib 4)
			// We get 4 instead of 5, because the left operand (which alters the shared context) will be entirely executed!
			// before the right one; 
		}

		LispExpression lambdaExpr = (LispExpression) globalLambdaFct.get(fctName);
		Iterator<LispItem> lambdaExprIt = lambdaExpr.values().iterator();
		lambdaExprIt.next();

		LispExpression lambdaParam = (LispExpression) lambdaExprIt.next();
		int lambdaParamSize = lambdaParam.values().size();

		LispItem lambdaFctBody = lambdaExprIt.next();
		if (!(lambdaFctBody instanceof LispExpression)) {
			return lambdaFctBody;
		}

		if (expression.values().size() - 1 != lambdaParamSize) {
			throw new LispError("Invalid number of parameters");
		}

		if (lambdaParamSize > 0) {
			Map<String, LispItem> paramMap = globalLambdaFctContext.get(parentLambdaFunctionContext); // retrieving altered function context
			if (paramMap == null) {
				paramMap = new HashMap<>();
			}

			Iterator<LispItem> lambdaParamIt = lambdaParam.values().iterator();
			LispItem givenParamTmp, lbdParamTmp;
			while (lambdaParamIt.hasNext()) {
				lbdParamTmp = lambdaParamIt.next();
				givenParamTmp = givenExprIt.next();
				if (!lbdParamTmp.equals(givenParamTmp)) {
					if (givenParamTmp instanceof LispExpression) {
						givenParamTmp = evaluateExpression((LispExpression) givenParamTmp);
					}
					paramMap.put(lbdParamTmp.toString(), givenParamTmp);
				}
			}
			globalLambdaFctContext.put(fctName, paramMap);
		}

		return evaluateExpression((LispExpression) lambdaFctBody);
	}

	private LispItem evaluateLambdaExpression(LispItem mapParamExprItem, String fctName) throws LispError {
		LispExpression lambdaExpr = (LispExpression) globalLambdaFct.get(fctName);
		Iterator<LispItem> lambdaExprIt = lambdaExpr.values().iterator();
		lambdaExprIt.next();

		LispExpression lambdaParam = (LispExpression) lambdaExprIt.next();
		int lambdaParamSize = lambdaParam.values().size();

		LispItem lambdaFctBody = lambdaExprIt.next();
		if (!(lambdaFctBody instanceof LispExpression)) {
			return lambdaFctBody;
		}

		if (lambdaParamSize > 0) {
			Map<String, LispItem> paramMap = new HashMap<>();
			Iterator<LispItem> lambdaParamIt = lambdaParam.values().iterator();
			paramMap.put(lambdaParamIt.next().toString(), mapParamExprItem); // support only one param at the moment!
			globalLambdaFctContext.put(fctName, paramMap);
		}

		return evaluateExpression((LispExpression) lambdaFctBody);
	}

	private LispItem evaluateMapExpression(LispExpression expression) throws LispError {
		Iterator<LispItem> it = expression.values().iterator();
		it.next();
		globalLambdaFct.put(LAMBDA_FUNCTION_TEMPORARY_NAME, (LispExpression) it.next());

		LispExpression result = new LispExpression();
		LispExpression mapParamExpr;
		LispItem tmp = it.next();
		String varName = tmp.toString();
		if (tmp instanceof LispIdentifier && globalVar.containsKey(varName)) {
			mapParamExpr = (LispExpression) globalVar.get(varName);
		} else if (tmp instanceof LispExpression) {
			mapParamExpr = (LispExpression) tmp;
		} else {
			throw new LispError("A List is expected!");
		}

		it = mapParamExpr.values().iterator();
		while (it.hasNext()) {
			result.append(evaluateLambdaExpression(it.next(), LAMBDA_FUNCTION_TEMPORARY_NAME));
		}

		globalLambdaFct.remove(LAMBDA_FUNCTION_TEMPORARY_NAME);
		return result;
	}

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
		Iterator<LispItem> it = expression.values().iterator();
		it.next();
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
		boolean operand = getBooleanValue(expression.nth(1)).value();
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

	/* helpers functions */
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
			if (!(item instanceof LispExpression)) {
				String id = ((LispIdentifier) item).toString();
				if (isLambdaFunction) {
					Map<String, LispItem> paramMap = globalLambdaFctContext.get(parentLambdaFunctionContext);
					if (paramMap.containsKey(id)) {
						return getNumericValue(paramMap.get(id));
					} else if (globalVar.containsKey(id)) {
						return getNumericValue(globalVar.get(id));
					} else {
						throw new LispError(id + " is undefined");
					}
				}
				if (globalVar.containsKey(id)) {
					return getNumericValue(globalVar.get(id));
				}
				if (id.matches("[+\\-]+\\d+(\\.\\d+)?([eE]\\-?\\d+)?")) {
					throw new LispError(id.charAt(0) + " should be a lisp operator");
				}
				throw new LispError("Not a number");
			}
			return (LispNumber) evaluateExpression((LispExpression) item);
		}
		return (LispNumber) item;
	}

	private LispBoolean getBooleanValue(LispItem item) throws LispError {
		if (!(item instanceof LispBoolean)) {
			LispItem elt = evaluateExpression((LispExpression) item);
			if (!(elt instanceof LispBoolean)) {
				if (elt instanceof LispIdentifier) {
					String id = ((LispIdentifier) item).toString();
					if (isLambdaFunction) {
						Map<String, LispItem> paramMap = globalLambdaFctContext.get(parentLambdaFunctionContext);
						if (paramMap.containsKey(id)) {
							return getBooleanValue(paramMap.get(id));
						} else {
							throw new LispError(id + " is undefined");
						}
					}
					if (globalVar.containsKey(id)) {
						return getBooleanValue(globalVar.get(id));
					}
				}
				throw new LispError("Not a Boolean");
			}
			return (LispBoolean) elt;
		}
		return (LispBoolean) item;
	}

	private LispItem getConsExpressionItemValue(LispItem item) throws LispError {
		if (item instanceof LispExpression) {
			return evaluateExpression((LispExpression) item);
		} else if (item instanceof LispIdentifier) {
			if (((LispIdentifier) item).equals("nil")) {
				return new LispExpression();
			}
			String id = ((LispIdentifier) item).toString();
			if (isLambdaFunction) {
				Map<String, LispItem> paramMap = globalLambdaFctContext.get(parentLambdaFunctionContext);
				if (paramMap.containsKey(id)) {
					return getConsExpressionItemValue(paramMap.get(id));
				} else {
					throw new LispError(id + " is undefined");
				}
			}
			if (globalVar.containsKey(id)) {
				return getConsExpressionItemValue(globalVar.get(id));
			}
		}
		return item;
	}

	private LispExpression processCarExpression(LispExpression expression) throws LispError {
		String operator = ((LispIdentifier) expression.values().car()).toString();
		if (operator.equals("cons")) {
			return evaluateConsExpression(expression);
		} else if (operator.equals("list")) {
			return evaluateListExpression(expression);
		} else {
			throw new LispError("Not a Cons");
		}
	}

	private boolean isKeyword(String s) {
		return LISP_KEYWORDS.contains(s);
	}

	private boolean isValidLambdaExpression(LispExpression expression) {
		int exprSize = expression.values().size();
		if (exprSize != 3) {
			return false;
		}

		Iterator<LispItem> it = expression.values().iterator();
		it.next();
		LispItem nextItem = it.next();
		if (!(nextItem instanceof LispExpression)) {
			return false;
		}
		return true;
	}

	private String extractLambdaExpression(LispExpression expression) {
		Iterator<LispItem> it = expression.values().iterator();
		StringBuilder bd = new StringBuilder();
		int exprSizeMoinsUn = expression.values().size() - 1;
		int cpt = 0;
		while (it.hasNext()) {
			bd.append(it.next().toString());
			if (cpt < exprSizeMoinsUn) {
				bd.append(" ");
			}
			cpt++;
		}
		return bd.toString();
	}
}
