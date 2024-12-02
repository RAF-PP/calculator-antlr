package rs.raf.calculator.ast;

import java.util.List;

import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper=true)
public final class VectorExpr extends Expr {
    private List<Expr> elements;

    public VectorExpr(Location location, List<Expr> elements) {
	super(location);
	this.elements = elements;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
	pp.node("vector", () -> elements.forEach(x -> x.prettyPrint(pp)));
    }
}
