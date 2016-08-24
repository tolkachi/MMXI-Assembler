package cse560;

public class ErrorHandler {
    public static void fatalError(String msg, int errorNum) {
        System.err.printf("[ERROR %03d] %s\n", errorNum, msg);
        System.exit(1);
    }

    public static void fatalError(String msg, int errorNum, int lineNumber) {
        ErrorHandler.fatalError(
                String.format("%s (Line: %d)", msg, lineNumber), errorNum);
    }
    
    public static void warning(String msg, int warningNum) {
    	System.err.printf("[WARNING %03d] %s\n", warningNum, msg);
    }
}
