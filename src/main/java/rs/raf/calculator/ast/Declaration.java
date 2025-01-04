package rs.raf.calculator.ast;

import java.util.Objects;

import lombok.*;

/** A variable declaration.  */
@Getter
@Setter
@EqualsAndHashCode(callSuper=true)
public sealed class Declaration extends Statement permits FunctionDeclaration {
    private String name;
    private Expr value;

    /** Type of the value stored in this variable.  */
    private Type declaredType;

    public Declaration(Location location, String name, Expr value) {
	super(location);
	this.name = name;
	this.value = value;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
	pp.node("declaration of %s".formatted(name),
		() -> {
                    if (getDeclaredType() != null) {
                        pp.terminal("type: %s"
                                    .formatted(getDeclaredType()
                                               .userReadableName()));
                    }
                    pp.terminal("identity: %s"
                                .formatted(Objects.toIdentityString(this)));
                    if (value != null)
                        value.prettyPrint(pp);
		});
    }
}
