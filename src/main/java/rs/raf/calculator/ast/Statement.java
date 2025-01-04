package rs.raf.calculator.ast;

/** Base class for all statements.  */
public sealed abstract class Statement extends Tree
        permits Declaration, ExprStmt, PrintStmt, ReturnStatement, StatementList {
    public Statement(Location location) {
        super(location);
    }
}
