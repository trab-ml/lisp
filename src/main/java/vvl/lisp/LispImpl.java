package vvl.lisp;

public class LispImpl implements Lisp {

	@Override
	public LispItem parse(String expr) throws LispError {
		String[] splitted = expr.split(" ");
		LispExpression newExpr = new LispExpression();
		LispItem currExpr;
		try {
			for (int i=splitted.length; i>=0; i--) {
				currExpr = LispBoolean.isLispBoolean(splitted[i]);
				if (currExpr != null) {
					newExpr.prepend(currExpr);
					continue;
				}
				
//				isLispIdentifier
				
				currExpr = LispNumber.isLispNumber(splitted[i]);
				if (currExpr != null) {
					newExpr.prepend(currExpr);
					continue;
				}
			}
		} catch (Exception e) {
			throw new LispError("Invalid lisp item.", e);
		}
		return newExpr;
	}

	@Override
	public LispItem evaluate(LispItem ex) throws LispError {
		// parse, evaluate and return a new LispItem which will contain the expected result
		return null;
	}

}
