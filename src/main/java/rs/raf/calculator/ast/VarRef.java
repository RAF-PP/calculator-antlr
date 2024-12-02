package rs.raf.calculator.ast;

import java.util.Objects;

import lombok.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper=true)
public final class VarRef extends Expr {
    private Declaration variable;

    protected VarRef(Location location, Declaration variable) {
        super(location);
        this.variable = variable;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
	pp.node("var", () -> {
                pp.terminal(variable.getName());
                pp.terminal("identity: %s"
                            /* toIdentityString is the default toString, so it
                               should return a string based on object
                               identity, i.e. a different one for each decl  */
                            .formatted(Objects.toIdentityString(variable)));
            });
    }
}
