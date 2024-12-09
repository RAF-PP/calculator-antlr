package rs.raf.calculator.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper= false)
@Getter
public class IntegerType implements Type {
    @Override
    public String userReadableName() {
        return "int";
    }
}