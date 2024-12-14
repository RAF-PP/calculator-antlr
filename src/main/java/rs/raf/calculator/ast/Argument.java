package rs.raf.calculator.ast;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class Argument extends Statement {
    private Type type;
    private final String identifier;

    public Argument(Location loc, String identifier, Type type) {
        super(loc);
        this.identifier = identifier;
        this.type = type;
    }

    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.node("Argument", () -> {
            if (type != null) {
                pp.terminal("type: " + type);
            }
            pp.terminal("identifier: " + identifier);
        });
    }
}
