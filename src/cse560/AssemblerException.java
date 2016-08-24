package cse560;

public class AssemblerException extends Exception {
    public enum Type {
        /** Indicates a fatal error. The program should terminate. */
        FATAL,

        /** Indicates a warning. Execution may continue. */
        WARNING;
    }

    /** The unique error number for this event. */
    private final int errorNumber;

    /** The line number in the program where the event occurred, if applicable. */
    private int lineNumber;

    public AssemblerException(String message, int errorNumber) {
        super(message);

        this.errorNumber = errorNumber;
    }

    public AssemblerException(String message, int errorNumber, int lineNumber) {
        this(message, errorNumber);

        this.lineNumber = lineNumber;
    }

    public int getErrorNumber() {
        return this.errorNumber;
    }

    public int getLineNumber() {
        return this.lineNumber;
    }
}
