package rs.raf.calculator.ast;

import java.util.Objects;

import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper=true)
public sealed class Expr extends Tree
        permits VarRef, VectorExpr, ErrorExpr, NumberLit {
    public enum Operation {
	ADD("+"),
	SUB("-"),

	MUL("*"),
	DIV("/"),

	POW("^"),

	/** A vector or a number or a variable.  */
	VALUE(null),
	;

	public final String label;

	Operation(String label) {
	    this.label = label;
	}
    }

    private Operation operation;
    private Expr lhs;
    private Expr rhs;

    private Type resultType;

    public Expr(Location location, Operation operation, Expr lhs, Expr rhs) {
        super(location);
	if (operation == Operation.VALUE)
	    throw new IllegalArgumentException("cannot construct a value like that");
	this.operation = operation;
	this.lhs = Objects.requireNonNull(lhs);
	this.rhs = Objects.requireNonNull(rhs);
    }

    protected Expr(Location location)
    {
	super(location);
	this.operation = Operation.VALUE;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
	pp.node(operation.label,
		() -> {
		    lhs.prettyPrint(pp);
		    rhs.prettyPrint(pp);

                    if (getResultType() != null) {
                        pp.node("type",
                                () ->
                                pp.terminal(getResultType()
                                            .userReadableName()));
                    }
		});
    }
}
