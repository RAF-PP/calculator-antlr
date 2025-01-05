package rs.raf.calculator.vm;

/** An upvalue slot can be populated from either a local variable or another
    upvalue.  Consider, for instance:

    {@snippet :
      fun sum(number a, number b): number {
        // 1
        fun do_sum(): number {
          // 2
          fun do_sum2(): number {
            // 3
            return a + b;
          }
          return do_sum2();
        }
        return do_sum();
      }
      }

    <p>
    In the example above, {@code sum} is a global variable, {@code do_sum} is a
    local variable in blob 1, as are {@code a} and {@code b}.

    <p>
    Blob 2 contains two upvalues, which are captures of {@code a} and {@code
    b}, both of which are populated from the locals table, from slot 0 and 1
    respectively, ergo, they had {@code UpvalueMapEntry(LOCAL, 0)} and {@code
    UpvalueMapEntry(LOCAL, 1)} in their upvalue map.

    <p>
    Blob 3 also contains an upvalue for {@code a} and {@code b}, but they are
    populated from the surrounding context's upvalue table, so they are instead
    represented by {@code UpvalueMapEntry(UPVALUE, 0)} and {@code
    UpvalueMapEntry(LOCAL, 1)}.

    @param loc Which table is the upvalue in?
    @param slot Which slot in that table is it in?  */
public record UpvalueMapEntry(UpvalueLocation loc,
                              int slot) {
    /** Where is this upvalue loaded from?  The closure context upvalue table
        or the closure context local table?  */
    public enum UpvalueLocation {
        UPVALUE, LOCAL;
    }

    /** Return a brief human-readable string describing this upvalue
        reference.  */
    public String format() {
        return "%c%d".formatted(loc.name().charAt(0), slot);
    }
}
