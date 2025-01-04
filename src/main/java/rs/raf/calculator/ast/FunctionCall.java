package rs.raf.calculator.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper=true)
public final class FunctionCall extends Expr {

    private Expr function;
    private List<Expr> arguments;

    public FunctionCall(Location location, Expr function, List<Expr> arguments) {
        super(location);
        this.arguments = arguments;
        this.function = function;
    }

    private Type resultType;

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.node(
                "FunctionCall",
                () -> {
                    pp.node("function", () -> function.prettyPrint(pp));

                    pp.node(
                            "arguments",
                            () -> arguments.forEach(arg -> arg.prettyPrint(pp))
                    );

                    if (getResultType() != null) {
                        pp.node(
                                "type",
                                () -> pp.terminal(getResultType().userReadableName())
                        );
                    }
                }
        );
    }
}
