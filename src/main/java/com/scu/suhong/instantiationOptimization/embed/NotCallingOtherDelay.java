// classes in "embed": their code are embedded in blockchain jar, while run in a separate process by system.exec
package com.scu.suhong.instantiationOptimization.embed;

public class NotCallingOtherDelay extends NotCallingOther {
    public NotCallingOtherDelay(String identifier) {
        super(identifier);
        logClassName = "NotCallingOtherDelay";
    }

    public static void main(String args[]) {
        doAction(args);
    }
    static void doAction(String[] args) {
        logClassName = "NotCallingOtherDelay";
        NotCallingOther.doAction(args);
    }
}
