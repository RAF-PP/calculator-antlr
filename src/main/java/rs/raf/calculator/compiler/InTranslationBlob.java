package rs.raf.calculator.compiler;

import java.util.IdentityHashMap;

import rs.raf.calculator.ast.Declaration;
import rs.raf.calculator.vm.Blob;
import rs.raf.calculator.vm.UpvalueMapEntry;
import lombok.*;

/** A function or toplevel bit of code currently under translation.  It
    <b>may</b> be associated with locals and upvalues, if in a function
    context.  Contains all the context required to translate a function,
    including its locals and upvalues, as well as the number of necessary local
    variables.

    The toplevel/global blob has no local variables, or upvalues.  */
@RequiredArgsConstructor
@Getter
public class InTranslationBlob {
    /** Code associated with this blob.  */
    private final Blob code;
    /** A mapping from declaration to the local variable slot.  If declaration
        {@code d} is mapped to {@code i}, then emitting {@code GET_LOCAL i}
        will result in the VM pushing the value of local variable {@code d}
        onto the operand stack.  Likewise, {@code SET_LOCAL i} will pop the
        top of the stack and have it become the value of {@code d}.  */
    private final IdentityHashMap<Declaration, Integer> localSlots;

    /** Each variable accessible as an upvalue gets mapeed to two things:
        <ul>
          <li>The upvalue table slot the variable resides in, and</li>
          <li>The mapping that describes how to populate that slot</li>
        </ul>

        These are convenient to store together, since they are associated with
        a single key, so this pair exists.

        @param slotNr Slot that this upvalue will be stored in.
        @param entry Upvalue map entry populating this slot.  */
    public record UpvalSlotInfo(int slotNr, UpvalueMapEntry entry) {}
    /** Mapping from a declaration to its slot number and the data for its
        upvalue map entry.  */
    private final IdentityHashMap<Declaration, UpvalSlotInfo> upvalSlots;
    /** Parent slot in the blob sphagetti-stack.  */
    private final InTranslationBlob previousBlob;
    /** Number of currently active local variables.  */
    private int localDepth = 0;
    /** Number of required local variable slots.  */
    private int maxLocalDepth = 0;

    /** Update the number of currently active locals.  Also updates {@link
        #maxLocalDepth}.  */
    public void setLocalDepth(int localDepth) {
        if ((this.localDepth = localDepth) > maxLocalDepth)
            maxLocalDepth = localDepth;
    }
}
