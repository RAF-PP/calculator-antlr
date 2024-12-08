package rs.raf.calculator.ast;

import calculator.parser.CalculatorParser;
import lombok.Getter;
import lombok.Setter;

public final class FunctionDeclaration extends Declaration {

    @Getter
    @Setter
    private Arguments args;
    private Type returnType;
    private String name;
    private StatementList body;

    public FunctionDeclaration (Location location, Arguments args, String name, StatementList body) {
        super (location, name, null);
        this.args = args;
        this.name = name;
        this.body = body;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.node("FunctionDeclaration", () -> {
            pp.terminal("name: " + name);
            pp.node("returnType", () -> {
                if (returnType != null) {
                    pp.terminal("type: " + returnType);
                } else {
                    pp.terminal("void");
                }
            });
            pp.node("args", () -> {
                args.prettyPrint(pp);
            });
            pp.node("body", () -> {
                body.prettyPrint(pp);
            });
        });
    }
}