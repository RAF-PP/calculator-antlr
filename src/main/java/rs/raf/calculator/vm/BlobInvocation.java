package rs.raf.calculator.vm;

import java.util.ArrayList;
import java.util.List;

import lombok.*;

/** Activation record of a blob. Contains the operand stack while executing
    this blob, the blob itself, as well as upvalues and locals (if
    applicable), as well as the IP to return to after executing.   */
@Data
@RequiredArgsConstructor
public class BlobInvocation {
    /** Stack of operands while executing this blob.  */
    private final List<Value> operandStack = new ArrayList<>();
    /** The blob being executed.  */
    private final Blob blob;
    /** The upvalue table.  Direct copy from the closure.  Immutable.  **/
    private final Value[] upvalues;
    /** The local value table.  */
    private final Value[] locals;
    /** The IP to return to.  */
    private final int prevIp;

    /** Construct a blob invocation for the toplevel blob.  It has no locals,
        nor upvalues (duh - there's no up).  */
    public BlobInvocation(Blob blob) {
        this(blob, null, null, -1);
    }
}
