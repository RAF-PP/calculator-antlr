package rs.raf.calculator.ast;

import java.util.Objects;

import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper=true)
public final class NumberLit extends Expr {
    private double value;

    public NumberLit(Location location, double value) {
	super(location);
	this.value = value;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
	pp.node("number", () -> pp.terminal(Objects.toString(value)));
    }

}
