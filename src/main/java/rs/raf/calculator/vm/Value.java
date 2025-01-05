package rs.raf.calculator.vm;

import java.io.PrintStream;
import java.util.List;

/** A value used by the VM.  */
public sealed interface Value {
    /** For {@link Instruction.Code#PRINT}, how do we print this value?

        @param out Stream to print into*/
    void print(PrintStream out);

    /** A numeric value.  */
    public record Number(double number) implements Value {
        @Override
        public void print(PrintStream out) {
            out.print(number);
        }
    }

    /** A list of other values.  */
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

    /** A closure value.  Contains the code to execute when executing this
        callable, as well as the number of locals as well as the upvalues
        captured at construction time.  */
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
