package vvl.lisp;

public enum ErrorMessage {
    INVALID_NUMBER_OF_OPERANDS("Invalid number of operands"),
    INVALID_BOOLEAN("Invalid boolean representation"),
    INVALID_IDENTIFIER(" is not a valid identifier"),
    INVALID_IMBRICATED_EXPRESSION("Invalid imbricated lambda expression"),
    NOT_A_NUMBER("Not a number"),
    UNDEFINED_VAR_MSG(" is undefined");
	
    private final String message;

    ErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
