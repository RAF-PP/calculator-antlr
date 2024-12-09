package rs.raf.calculator.ast;

public class ArrType implements Type {
    private Type elementType;

    public ArrType (Type elementType)
    {
        this.elementType = elementType;
    }

    @Override
    public String userReadableName() {
        return "arr[" + elementType.userReadableName() + "]";
    }
}