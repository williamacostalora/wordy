package wordy.ast;

import wordy.interpreter.EvaluationContext;
import wordy.interpreter.LoopExited;

import java.util.Collections;
import java.util.Map;

/**
 * A statement that causes program flow to exit the nearest-nested loop. Often called “break” in
 * other languages.
 * 
 * The interpreter implements this by throwing a `LoopExited` exception.
 */
public final class LoopExitNode extends StatementNode {
    public LoopExitNode() {
    }

    @Override
    public Map<String, ASTNode> getChildren() {
        return Collections.emptyMap();
    }
    @Override
    public boolean equals(Object o) {
        return this == o
            || o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "LoopExitNode";
    }

    @Override
    protected void doRun(EvaluationContext context) {
        throw new LoopExited();
    }
}
