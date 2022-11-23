package util;

public class FunctionHelper {
    public static String getFunctionName() {
        StackTraceElement traceElement = ((new Exception()).getStackTrace())[1];
        return traceElement.getMethodName();
    }
}
