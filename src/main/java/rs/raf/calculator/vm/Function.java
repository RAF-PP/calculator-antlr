package rs.raf.calculator.vm;

import lombok.Data;
import rs.raf.calculator.ast.FunctionDeclaration;

/** A VM function.  A function is effectively a blueprint for a closure: it
    specifies which locals of the surrounding context end up in which part of
    the local table for this function.  That local table then gets cloned with
    the arguments to form the actual function local table.
 */
@Data
public class Function {
    private Blob code;
    /** Given {@code upvalueMap[i] = x}, upvalue in slot {@code i} will be
        loaded with upvalue in {@code CLOSURE} context slot {@code x.slot()} if
        {@code x.loc()} is {@code UPVALUE}, or local variable in local slot
        {@code x.slot()}.

        In essence, we initialize upvalue {@code i} with either the upvalue or
        local variable {@code x.slot()} at time when we construct the closure,
        propagating it into the closure.  */
    private UpvalueMapEntry[] upvalueMap;
    /** Number of local variables in this function.  */
    private int localCount = -1;
    private FunctionDeclaration funcDecl;
}
