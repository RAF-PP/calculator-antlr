package rs.raf.calculator.ast;

import lombok.*;

/** A type for all numeric values.  */
@Getter
@Setter
@EqualsAndHashCode
public class NumberType implements Type {
    @Override
    public String userReadableName() {
        return "number";
    }
}
