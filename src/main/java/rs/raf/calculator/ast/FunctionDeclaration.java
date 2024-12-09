package rs.raf.calculator.ast;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class FunctionDeclaration extends Declaration {


    private Arguments args;
    private Type returnType;
    private String name;
    private StatementList body;

    public FunctionDeclaration (Location location, Arguments args, String name, StatementList body, Type returnType) {
        super (location, name, null);
        this.args = args;
        this.name = name;
        this.body = body;
        this.returnType = returnType;
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.node("FunctionDeclaration", () -> {
            pp.terminal("name: " + name);
            pp.node("returnType", () -> {
                if (returnType != null) {
                    pp.terminal("type: " + returnType.userReadableName());
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