package rs.raf.calculator.vm;

import java.util.ArrayList;
import java.util.List;

/** A bunch of code that the VM can execute.  */
public record Blob(List<Instruction> code,
                   /** {@code PUSH_CONSTANT} instructions contain a reference
                       by index into this table.  */
                   List<Double> constantTable)
{
    public Blob() {
        this(new ArrayList<>(), new ArrayList<>());
    }

    public int addInsn(Instruction instruction) {
        var newInsnIp = code().size();
        code().add(instruction);
        return newInsnIp;
    }
}
