package rs.raf.calculator.vm;

public record UpvalueMapEntry(UpvalueLocation loc,
                              int slot) {
    public enum UpvalueLocation {
        UPVALUE, LOCAL;
    }

    public String format() {
        return "%c%d".formatted(loc.name().charAt(0), slot);
    }
}
