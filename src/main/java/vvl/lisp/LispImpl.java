package vvl.lisp;

import java.util.Arrays;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LispImpl implements Lisp {

	private Map<String, LispItem> globalVar = new HashMap<>();
	private Map<String, LispExpression> globalLambdaFct = new HashMap<>();
	private Map<String, Map<String, LispItem>> globalLambdaFctContext = new HashMap<>();
	private static final Set<String> LISP_KEYWORDS = new HashSet<>(Arrays.asList("or", "not", "and", "lambda", "define",
			"set!", "cons", "#t", "#f", "nil", "car", "cdr", "list"));
	private static final String LAMBDA_FUNCTION_TEMPORARY_NAME = "TEMPORARY_MAP_EXPRESSION_LAMBDA_FUNCTION_NAME";
	
	private boolean isLambdaFunction = false;
	private String parentLambdaFunctionContext = "";

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

	@Override
	public LispItem evaluate(LispItem ex) throws LispError {
		if (ex instanceof LispBoolean) {
			return ex;
		} else if (ex instanceof LispIdentifier) {
			return evaluateIdentifier((LispIdentifier) ex);
		} 
		return evaluateExpression((LispExpression) ex);
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
		throw new LispError(id, ErrorMessage.UNDEFINED_VAR_MSG);
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
				throw new LispError(ErrorMessage.INVALID_NUMBER_OF_OPERANDS);
			default:
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
				if (expressionSize == 3) {
					Iterator<LispItem> exprIt = expression.values().iterator();
					exprIt.next();
					LispItem leftOp = exprIt.next();
					LispItem rightOp = exprIt.next();
					if ((leftOp instanceof LispExpression) && (rightOp instanceof LispExpression)) {
						LispExpression result = new LispExpression((LispIdentifier) operatorItem);
						result.append(extractParamsOfExpressionsAtTheSameLevel((LispExpression) leftOp));
						result.append(extractParamsOfExpressionsAtTheSameLevel((LispExpression) rightOp));
						return evaluateExpression(result);
					}
				}
				evaluatedExpr = evaluateArithmeticExpression(expression);
			} else if (operator.equals("cons")) {
				if (expressionSize < 3) {
					throw new LispError(ErrorMessage.INVALID_NUMBER_OF_OPERANDS);
				}
				evaluatedExpr = evaluateConsExpression(expression);
			} else if (operator.equals("quote")) {
				if (expressionSize != 2) {
					throw new LispError(ErrorMessage.INVALID_NUMBER_OF_OPERANDS);
				}
				evaluatedExpr = evaluateQuoteExpression(expression);
			} else if (operator.equals("list")) {
				evaluatedExpr = evaluateListExpression(expression);
			} else if (operator.equals("if")) {
				if (expressionSize != 4) {
					throw new LispError(ErrorMessage.INVALID_NUMBER_OF_OPERANDS);
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
				evaluatedExpr = evaluateLambdaExpression(expression);
				isLambdaFunction = false;
				parentLambdaFunctionContext = "";
			} else if (isLambdaFunction && globalLambdaFct.containsKey(parentLambdaFunctionContext)) {
				evaluatedExpr = evaluateLambdaExpression(expression);
			} else {
				throw new LispError(operator, ErrorMessage.UNDEFINED_VAR_MSG);
			}
			return evaluatedExpr;
		} else if (operatorItem instanceof LispExpression) {
			return evaluateLambdaExpressionOfLambdaExpression(expression);
		} else {
			throw new LispError("Invalid expression -> " + expression.toString());
		}
	}

	private LispBoolean evaluateComparisonExpression(LispExpression expression) throws LispError {
		LispItem operatorItem = expression.values().car();
		String operator = ((LispIdentifier) operatorItem).toString();
		if (expression.values().size() < 2) {
			throw new LispError(ErrorMessage.INVALID_NUMBER_OF_OPERANDS);
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
		if (exprLenght == 2 && operator.equals("-")) {
			return result.multiply(new LispNumber(-1));
		}
		if (exprLenght != 3 && (operator.equals("-") || operator.equals("/"))) {
			throw new LispError(ErrorMessage.INVALID_NUMBER_OF_OPERANDS);
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

	private LispItem evaluateQuoteExpression(LispExpression expression) {
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
			throw new LispError(ErrorMessage.INVALID_NUMBER_OF_OPERANDS);
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
			throw new LispError(ErrorMessage.INVALID_NUMBER_OF_OPERANDS);
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
			throw new LispError(varName, ErrorMessage.INVALID_IDENTIFIER);
		}
		
		if (!varName.matches("[a-zA-Z]+\\w*") || isKeyword(varName) || globalVar.containsKey(varName)) {
			throw new LispError(varName, ErrorMessage.INVALID_IDENTIFIER);
		}
		nextItem = it.next();
		if (nextItem instanceof LispExpression) {
			LispExpression expr = (LispExpression) nextItem;
			if (isValidLambdaExpression(expr)) {
				globalLambdaFct.put(varName, expr);
				return new LispIdentifier(extractLambdaExpression(expr));
			}
			nextItem = evaluateExpression(expr);
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
			throw new LispError(varName, ErrorMessage.INVALID_IDENTIFIER);
		}
		if (!varName.matches("[a-zA-Z]+\\w*") || isKeyword(varName)) {
			throw new LispError(varName, ErrorMessage.INVALID_IDENTIFIER);
		}
		if (globalVar.containsKey(varName)) {
			nextItem = it.next();
			if (nextItem instanceof LispExpression) {
				nextItem = evaluateExpression((LispExpression) nextItem);
			}
			globalVar.put(varName, nextItem);
			return nextItem;
		}
		throw new LispError(varName, ErrorMessage.UNDEFINED_VAR_MSG);
	}

	private LispItem evaluateLambdaExpression(LispExpression expression) throws LispError {
		Iterator<LispItem> givenExprIt = expression.values().iterator();
		String fctName = ((LispIdentifier) givenExprIt.next()).toString();
		if (!globalLambdaFct.containsKey(fctName)) {
			fctName = globalLambdaFctContext.get(parentLambdaFunctionContext).get(fctName).toString();
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

		if (lambdaParamSize > 0) {
			Map<String, LispItem> paramMap;
			if (globalLambdaFctContext.containsKey(parentLambdaFunctionContext)) {
				paramMap = globalLambdaFctContext.get(parentLambdaFunctionContext);
			} else {
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
			globalLambdaFctContext.put(parentLambdaFunctionContext, paramMap);
		}
		return evaluateExpression((LispExpression) lambdaFctBody);
	}

	private LispItem evaluateLambdaExpressionOfLambdaExpression(LispExpression expression) throws LispError {
		Iterator<LispItem> givenExprIt = expression.values().iterator();

		LispExpression firstExpr = (LispExpression) givenExprIt.next();
		Iterator<LispItem> firstExprIt = firstExpr.values().iterator();
		String lambdaFctName = ((LispIdentifier) firstExprIt.next()).toString();
		if (!globalLambdaFct.containsKey(lambdaFctName)) {
			throw new LispError("Unknown function");
		}

		LispExpression lambdaExpr = globalLambdaFct.get(lambdaFctName);
		Iterator<LispItem> lambdaExprIt = lambdaExpr.values().iterator();
		lambdaExprIt.next();

		LispExpression lambdaParam = (LispExpression) lambdaExprIt.next();
		int lambdaParamSize = lambdaParam.values().size();

		LispExpression lambdaFctBody = (LispExpression) lambdaExprIt.next();
		if (!isValidLambdaExpression(lambdaFctBody)) {
			throw new LispError(ErrorMessage.INVALID_IMBRICATED_EXPRESSION);
		}

		if (expression.values().size() - 1 != lambdaParamSize) {
			throw new LispError(ErrorMessage.INVALID_NUMBER_OF_OPERANDS);
		}

		// Mapping parameters of first item
		LispItem givenParamTmp;
		LispItem lbdParamTmp;
		Map<String, LispItem> paramMap = new HashMap<>();
		Iterator<LispItem> lambdaParamIt;
		if (lambdaParamSize > 0) {
			lambdaParamIt = lambdaParam.values().iterator();
			while (lambdaParamIt.hasNext()) {
				lbdParamTmp = lambdaParamIt.next();
				givenParamTmp = firstExprIt.next();
				if (!lbdParamTmp.equals(givenParamTmp)) {
					if (givenParamTmp instanceof LispExpression) {
						givenParamTmp = evaluateExpression((LispExpression) givenParamTmp);
					}
					paramMap.put(lbdParamTmp.toString(), givenParamTmp);
				}
			}
			globalLambdaFctContext.put(lambdaFctName, paramMap);
		}

		// Mapping parameters of remaining items
		LispExpression lambdaFctBodyClone = lambdaFctBody;
		Iterator<LispItem> lambdaFctBodyCloneIt = lambdaFctBodyClone.values().iterator();
		lambdaFctBodyCloneIt.next();
		lambdaParamIt = ((LispExpression) lambdaFctBodyCloneIt.next()).values().iterator();
		lambdaFctBody = (LispExpression) lambdaFctBodyCloneIt.next();
		while (givenExprIt.hasNext()) {
			if (!lambdaParamIt.hasNext()) { // lambdaFctBodyClone is a lambda expression!
				lambdaFctBodyClone = (LispExpression) lambdaFctBodyCloneIt.next();
				if (!isValidLambdaExpression(lambdaFctBodyClone)) {
					throw new LispError(ErrorMessage.INVALID_IMBRICATED_EXPRESSION);
				}
				lambdaFctBodyCloneIt = lambdaFctBodyClone.values().iterator();
				lambdaFctBodyCloneIt.next();
				lambdaParamIt = ((LispExpression) lambdaFctBodyCloneIt.next()).values().iterator();
				lambdaFctBody = (LispExpression) lambdaFctBodyCloneIt.next();
			}
			lbdParamTmp = lambdaParamIt.next();
			givenParamTmp = givenExprIt.next();
			if (!lbdParamTmp.equals(givenParamTmp)) {
				if (givenParamTmp instanceof LispExpression) {
					givenParamTmp = evaluateExpression((LispExpression) givenParamTmp);
				}
				paramMap.put(lbdParamTmp.toString(), givenParamTmp);
			}
			globalLambdaFctContext.put(lambdaFctName, paramMap);
		}

		isLambdaFunction = true;
		parentLambdaFunctionContext = lambdaFctName;
		LispItem result = evaluateExpression(lambdaFctBody);
		isLambdaFunction = false;
		parentLambdaFunctionContext = "";
		return result;
	}

	private LispItem evaluateMapExpression(LispExpression expression) throws LispError {
		Iterator<LispItem> it = expression.values().iterator();
		it.next();

		String fctName = LAMBDA_FUNCTION_TEMPORARY_NAME;
		LispItem firstItem = it.next();
		if (firstItem instanceof LispIdentifier) {
			fctName = firstItem.toString();
		} else {
			globalLambdaFct.put(fctName, (LispExpression) firstItem);
		}

		LispExpression result = new LispExpression();
		LispExpression mapParamExpr;
		LispItem tmp = it.next();
		String varName = tmp.toString();

		if (tmp instanceof LispIdentifier && globalVar.containsKey(varName)) {
			mapParamExpr = (LispExpression) globalVar.get(varName);
		} else if (tmp instanceof LispExpression) {
			mapParamExpr = (LispExpression) evaluateExpression((LispExpression) tmp);
		} else {
			throw new LispError("A List is expected!");
		}

		isLambdaFunction = true;
		parentLambdaFunctionContext = fctName;
		it = mapParamExpr.values().iterator();
		while (it.hasNext()) {
			LispExpression temp = new LispExpression(new LispIdentifier(fctName));
			temp.append(it.next());
			result.append(evaluateExpression(temp));
		}
		isLambdaFunction = false;
		parentLambdaFunctionContext = "";

		if (globalLambdaFct.containsKey(LAMBDA_FUNCTION_TEMPORARY_NAME)) {
			globalLambdaFct.remove(LAMBDA_FUNCTION_TEMPORARY_NAME);
		}
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
			throw new LispError(ErrorMessage.INVALID_NUMBER_OF_OPERANDS);
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
						throw new LispError(id, ErrorMessage.UNDEFINED_VAR_MSG);
					}
				}

				if (globalVar.containsKey(id)) {
					return getNumericValue(globalVar.get(id));
				}
				if (id.matches("[+\\-]+\\d+(\\.\\d+)?([eE]\\-?\\d+)?")) {
					throw new LispError(id.charAt(0) + " should be a lisp operator");
				}
				throw new LispError(ErrorMessage.NOT_A_NUMBER);
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
							throw new LispError(id, ErrorMessage.UNDEFINED_VAR_MSG);
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
			if ("nil".equals(((LispIdentifier) item).toString())) {
				return new LispExpression();
			}
			String id = ((LispIdentifier) item).toString();
			if (isLambdaFunction) {
				Map<String, LispItem> paramMap = globalLambdaFctContext.get(parentLambdaFunctionContext);
				if (paramMap.containsKey(id)) {
					return getConsExpressionItemValue(paramMap.get(id));
				} else {
					throw new LispError(id, ErrorMessage.UNDEFINED_VAR_MSG);
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

	private boolean isValidLambdaExpression(LispExpression expression) throws LispError {
		int exprSize = expression.values().size();
		Iterator<LispItem> it = expression.values().iterator();
		LispItem id = it.next();
		
		if ("lambda".equals(id.toString())) {
			if (exprSize != 3) {
				throw new LispError(ErrorMessage.INVALID_NUMBER_OF_OPERANDS);
			}
			
			LispItem nextItem = it.next();
			if (!(nextItem instanceof LispExpression)) {
				return false;
			}
			return true;
		}
		
		return false;
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

	private LispItem extractParamsOfExpressionsAtTheSameLevel(LispExpression expression) throws LispError {
		Iterator<LispItem> exprIt = expression.values().iterator();
		LispIdentifier id = (LispIdentifier) exprIt.next();
		LispItem potentialExpr = exprIt.next();
		if (globalLambdaFct.containsKey(id.toString()) && (potentialExpr instanceof LispExpression)) {
			LispExpression result = new LispExpression(id);
			result.append(evaluateExpression(((LispExpression) potentialExpr)));
			return result;
		}
		return evaluateExpression(expression);
	}
}
