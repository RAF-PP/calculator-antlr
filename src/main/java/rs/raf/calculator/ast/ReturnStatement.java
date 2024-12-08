package rs.raf.calculator.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
public final class ReturnStatement extends Statement
{
    private Expr value;

    public ReturnStatement (Location location, Expr value)
    {
        super (location);
        this.value = value;
    }


    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.node("ReturnStatement", () -> {
            if (value != null) {
                pp.node("value", () -> value.prettyPrint(pp));
            } else {
                pp.terminal("value: null");
            }
        });
    }
}
