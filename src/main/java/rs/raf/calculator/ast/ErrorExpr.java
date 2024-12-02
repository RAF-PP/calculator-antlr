package rs.raf.calculator.ast;

/** A special node indicating an error occurred.  */
public class ErrorExpr extends Expr {
    protected ErrorExpr(Location location) {
        super(location);
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
	pp.node("error", () -> {});
    }
}
