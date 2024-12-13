package rs.raf.calculator.ast;

import java.util.ArrayList;
import java.util.List;

public final class Arguments extends Statement{
    private final List<Argument> arguments;

    public Arguments(Location location, List<Argument> arguments) {
        super(location);
        this.arguments = new ArrayList<>(arguments);
    }

    @Override
    public void prettyPrint(ASTPrettyPrinter pp) {
        pp.node("Arguments", () -> {
            for (Argument argument : arguments) {
                argument.prettyPrint(pp);
            }
        });
    }
}
