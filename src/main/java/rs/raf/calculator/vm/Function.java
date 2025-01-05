package rs.raf.calculator.vm;

import lombok.Data;
import rs.raf.calculator.ast.FunctionDeclaration;

/** A VM function.  A function is effectively a blueprint for a closure: it
    has information on how to construct a {@link Value.Closure}.

    <p> Notably, it has the code for the closure, a description of how to
    obtain the upvalues to store in the closure, and the length of the local
    table.  For debugging, it also has a reference to the function declaration
    this function is derived from.  */
@Data
public class Function {
    /** Code to place in the closure.  */
    private Blob code;
    /** Given {@code upvalueMap[i] = x}, upvalue in slot {@code i} will be
        loaded with upvalue in slot {@code x.slot()} of the upvalue table of
        the currently executing function, if {@code x.loc()} is {@code
        UPVALUE}, or local variable in local slot {@code x.slot()}, at the time
        of executing {@link Instruction.Code#BUILD_CLOSURE
        <code>BUILD_CLOSURE</code>}.

        <p> In essence, we initialize upvalue {@code i} with either the upvalue
        or local variable {@code x.slot()} at time when executing {@link
        Instruction.Code#BUILD_CLOSURE <code>BUILD_CLOSURE</code>} depending on
        the value of {@code x.loc()}.

        <p> For example, if, at time of executing the {@code BUILD_CLOSURE}
        instruction, we have a local value {@code a} in local slot {@code 4},
        and an upvalue {@code b} in upvalue slot {@code 5}, and we have {@code
        upvalueMap} with {@code {(LOCAL, 4), (UPVALUE, 5)}}, then, in the new
        closure, upvalue 0 will be {@code a}, and upvalue 1 will be {@code
        b}.

        This language supports function nesting, but, as a result of the
        laziness of the author, however, values are all immutable, but this
        does mean that it is appropriate to copy values in order to implement
        upvalues.  */
    private UpvalueMapEntry[] upvalueMap;
    /** Number of local variables in this function.  */
    private int localCount = -1;
    /** Function declaration this function is derived from.  */
    private FunctionDeclaration funcDecl;
}
