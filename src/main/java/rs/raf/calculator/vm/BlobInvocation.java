package rs.raf.calculator.vm;

import java.util.ArrayList;
import java.util.List;

import lombok.*;

@Data
@RequiredArgsConstructor
public class BlobInvocation {
    private final List<Value> operandStack = new ArrayList<>();
    private final Blob blob;
    private final Value[] upvalues;
    private final Value[] locals;
    private final int prevIp;

    /** Construct a blob invocation for the outermost blob.  It has no locals,
        nor upvalues (duh - there's no up).  */
    public BlobInvocation(Blob blob) {
        this(blob, null, null, -1);
    }
}
