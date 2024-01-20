package vvl.lisp;

import java.util.Iterator;

public class LispImpl implements Lisp {

	@Override
	public LispItem parse(String expr) throws LispError {
		// presence of ()
		// an expr is constituted of ListItem, it start with '(' and
		// finish with ')'
		// it could be formed of many expr, so it is a particular ListItem
		// As return we could have any kind of ListItem?? not a LispExpression only??
		return null;
	}

	@Override
	public LispItem evaluate(LispItem ex) throws LispError {
		// evaluate and return a new LispItem which will contain the expected result
		if (ex instanceof LispNumber) {
			return ex;
		} else if (ex instanceof LispBoolean) {
			return ((LispBoolean) ex).value() ? new LispNumber(1) : new LispNumber(0);
		} else if (ex instanceof LispExpression) {
			return null;
		} else {
			throw new LispError("Unsupported LispItem of type " + ex.getClass().getSimpleName());
		}
	}

}
