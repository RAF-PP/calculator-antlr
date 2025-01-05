package rs.raf.calculator.vm;

import java.util.ArrayList;
import java.util.List;

/** A bunch of code that the VM can execute.  May or may not be associated
    with a function, may or may not have locals.

    @param code Code that will be executed for this blob.  Must terminate in
           either a {@link Instruction.Code#RETURN <code>RETURN</code>}, {@link
           Instruction.Code#RETURN_VOID <code>RETURN_VOID</code>} or {@link
           Instruction.Code#FINISH_OUTER <code>FINISH_OUTER</code>}.
    @param constantTable Table of constant values referenced by the {@link
           Instruction.Code#PUSH_CONSTANT <code>PUSH_CONSTANT</code>}
           instructions.  */
public record Blob(List<Instruction> code,
                   List<Double> constantTable)
{
    public Blob() {
        this(new ArrayList<>(), new ArrayList<>());
    }

    /** @param instruction Instruction to append to this blob.

        @return IP of the new instruction.  */
    public int addInsn(Instruction instruction) {
        var newInsnIp = code().size();
        code().add(instruction);
        return newInsnIp;
    }
}
