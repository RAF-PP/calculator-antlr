package rs.raf.calculator.vm;

import java.io.PrintStream;
import java.util.List;

public sealed interface Value {
    void print(PrintStream out);

    public record Number(double number) implements Value {
        @Override
        public void print(PrintStream out) {
            out.print(number);
        }
    }
    public record Vector(List<Value> elements) implements Value {
        @Override
        public void print(PrintStream out) {
            out.print('[');
            var first = true;
            for (var e : elements) {
                if (!first) out.print(", ");
                first = false;
                e.print(out);
            }
            out.print(']');
        }
    }

    /** This language supports function nesting.  As a result of the laziness
        of the author, however, these values are all immutable.  */
    public record Closure(Blob code,
                          Value[] upvalues,
                          int localCount)
        implements Value
    {
        @Override
        public void print(PrintStream out) {
            out.printf("<function %s>", System.identityHashCode(code));
        }
    }
}
