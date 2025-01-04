package rs.raf.calculator.compiler;

import java.util.IdentityHashMap;

import rs.raf.calculator.ast.Declaration;
import rs.raf.calculator.vm.Blob;
import rs.raf.calculator.vm.UpvalueMapEntry;
import lombok.*;

@RequiredArgsConstructor
@Getter
public class InTranslationBlob {
    private final Blob code;
    private final IdentityHashMap<Declaration, Integer> localSlots;

    public record UpvalSlotInfo(int slotNr, UpvalueMapEntry entry) {}
    private final IdentityHashMap<Declaration, UpvalSlotInfo> upvalSlots;
    private final InTranslationBlob previousBlob;
    private int localDepth = 0;
    private int maxLocalDepth = 0;

    public void setLocalDepth(int localDepth) {
        if ((this.localDepth = localDepth) > maxLocalDepth)
            maxLocalDepth = localDepth;
    }
}
