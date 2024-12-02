package rs.raf.calculator.ast;

import lombok.*;

@Getter
@Setter
@EqualsAndHashCode
public class ListType implements Type {
    private Type elementType;

    public ListType(Type elementType) {
        this.elementType = elementType;
    }

    @Override
    public String userReadableName() {
        /* number[] for instance.  */
        return elementType.userReadableName() + "[]";
    }
}
