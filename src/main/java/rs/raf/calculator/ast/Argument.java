package rs.raf.calculator.ast;

public final class Argument extends Statement {
    private Type type;
    private final String identifier;

    public Argument(Location loc, String identifier) {
        super(loc);
        this.identifier = identifier;

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
