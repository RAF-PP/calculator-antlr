package rs.raf.calculator.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode
public class FunctionType implements Type {
    private Type returnType;
    private List<Type> argumentTypes;

    public FunctionType(Type returnType, List<Type> argType)
    {
        this.returnType = returnType;
        this.argumentTypes = argType;
    }

    @Override
    public String userReadableName() {
        // Generate a comma-separated list of argument types
        String args = argumentTypes.stream()
                .map(Type::userReadableName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");

        // Return a function signature like "(arg1Type, arg2Type) -> returnType"
        return "(" + args + ") -> " + returnType.userReadableName();
    }
}
