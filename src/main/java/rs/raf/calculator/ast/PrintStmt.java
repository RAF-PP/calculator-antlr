package rs.raf.calculator.ast;

import java.util.List;

import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper=true)
/** Prints values of expressions.  */
public final class PrintStmt extends Statement {
    private List<Expr> args;

    public PrintStmt(Location location, List<Expr> args) {
        super(location);
	this.args = args;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
	pp.node("print",
		() -> {
		    args.forEach(x -> x.prettyPrint(pp));
		});
    }
}
