package rs.raf.calculator.ast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.*;

import calculator.parser.CalculatorParser.*;
import calculator.parser.CalculatorLexer;
import calculator.parser.CalculatorVisitor;

import rs.raf.calculator.Calculator;

public class CSTtoASTConverter extends AbstractParseTreeVisitor<Tree> implements CalculatorVisitor<Tree> {
    private Calculator c;

    public CSTtoASTConverter(Calculator calculator) {
        this.c = calculator;
        /* Open the global scope.  */
        openBlock();
    }

    @Override
    public Tree visitStart(StartContext ctx) {
        /* We don't open a new scope here, because we should be in the global
           scope we opened in the constructor.  */
        var stmts = ctx.statement()
            /* Take all the parsed statements, ... */
            .stream()
            /* ... visit them using this visitor, ... */
            .map(this::visit)
            /* ... then cast them to statements (because 'start: statement*',
               so they can't be anything else), ...  */
            .map(x -> (Statement) x)
            /* ... and put them into a list.  */
            .collect(Collectors.toCollection(ArrayList::new));
        return new StatementList(getLocation(ctx), stmts);
    }

    /* A stack of environments.  */
    private List<Map<String, Declaration>> environments = new ArrayList<>();

    /** Open a new scope. */
    private void openBlock() {
        environments.add(new HashMap<>());
    }

    /** Removes the last scope. */
    private void closeBlock() {
        environments.removeLast();
    }

    /** Saves a declaration into the current environment, diagnosing
        redeclaration. */
    private void pushDecl(String name, Declaration decl) {
        /* Intentionally overwriting the old variable as error recovery.  */
        var oldDecl = environments.getLast().put(name, decl);
        if (oldDecl != null) {
            c.error(decl.getLocation(), "");
        }
    }

    /** Tries to find a declaration in any scope parent to this one.  */
    private Optional<Declaration> lookup(Location loc, String name) {
        /* Walk through the scope, starting at the top one, ... */
        for (var block : environments.reversed()) {
            /* ... for each of them, try to find the name we're looking for in
               the environment... */
            var decl = block.get(name);

            if (decl != null) {
                /* ... and if it is found, return it....  */
                return Optional.of(decl);
            }
        }
        /* ... otherwise, we fell through and found nothing.  Diagnose and
           continue.  */
        c.error(loc, "failed to find variable '%s' in current scope", name);
        return Optional.empty();
    }

    @Override
    public Tree visitBlock(BlockContext ctx) {
        /* Open a new environment.  */
        openBlock();

        var stmts = ctx.statement()
            /* Take all the parsed statements, ... */
            .stream()
            /* ... visit them using this visitor, ... */
            .map(this::visit)
            /* ... then cast them to statements (because 'start: statement*',
               so they can't be anything else), ...  */
            .map(x -> (Statement) x)
            /* ... and put them into a list.  */
            .collect(Collectors.toCollection(ArrayList::new));

        /* Close the one opened above.  */
        closeBlock();
        return new StatementList(getLocation(ctx), stmts);
    }

    @Override
    public Tree visitStatement(StatementContext ctx) {
        /* A statement just contains a child.  Visit it instead.  It has to be
           a statement, so we check for that by casting.

           Note that we assume here that statement is defined as an OR of many
           different rules, with its first child being whatever the statement
           actually is, and the rest, if any, are unimportant.  */
        var substatement = visit(ctx.getChild(0));
        if (substatement instanceof Expr e) {
            /* It's an expression statement.  */
            substatement = new ExprStmt(e.getLocation(), e);
        }
        return (Statement) substatement;
    }

    @Override
    public Tree visitDeclaration(DeclarationContext ctx) {
        /* Declarations consist of a name and an expression, which is the value
           of the variable we're declaring.  */
        var name = ctx.IDENTIFIER().getText();
        var value = (Expr) visit(ctx.expr());

        /* Intentionally after the visit above in order to not declare the
           value in its right-hand side.  */
        var decl = new Declaration(getLocation(ctx), name, value);
        /* Save the declaration we just parsed.  */
        pushDecl(name, decl);

        return decl;
    }

    @Override
    public Tree visitDeclareFunction(DeclareFunctionContext ctx) {
        var name = ctx.IDENTIFIER().getText();
        var args = (Arguments) visitArglist(ctx.arglist());
        var declLoc = getLocation(ctx.start).span(getLocation(ctx.retT.start));

        var body = (StatementList) visit(ctx.body);

        var funDecl = new FunctionDeclaration(declLoc, args, name, body);
        pushDecl(name, funDecl);

        return funDecl;
    }

    @Override
    public Tree visitArglist(ArglistContext ctx) {
        // Visit each argument in the list and collect the results into a list
        var arguments = ctx.argument()
                .stream()
                .map(this::visit)
                .map(x -> (Argument) x) // Cast each visited result to an Argument
                .collect(Collectors.toCollection(ArrayList::new));

        // Create an ArgumentList node from the collected arguments
        return new Arguments(getLocation(ctx), arguments);
    }

    @Override
    public Tree visitArgument(ArgumentContext ctx) {
        // Extract the type and identifier for the argument
        String identifier = ctx.IDENTIFIER().getText();
        Declaration decl = new Declaration(getLocation(ctx.start), identifier, null);
        pushDecl(identifier, decl);
        // Create and return an Argument node
        return new Argument(getLocation(ctx), identifier);
    }

    @Override
    public Tree visitReturnStmt(ReturnStmtContext ctx) {
        var op = (Expr) visit(ctx.expr());
        return new ReturnStatement(getLocation(ctx.RETURN()), op);
    }

    @Override
    public Tree visitTypeid(TypeidContext ctx) {
        if (ctx.INT_TYPE() != null) {
            return visit(ctx.INT_TYPE());
        }
        if (ctx.arrType() != null) {
            visit(ctx.arrType());
        }

        throw new AssertionError ("Bad type");
    }

    @Override
    public Tree visitArrType(ArrTypeContext ctx) {
        return visit(ctx.typeid());
    }

    @Override
    public Tree visitPrintStatement(PrintStatementContext ctx) {
        var args = ctx.expr()
            /* Take all the parsed arguments, ... */
            .stream()
            /* ... visit them using this visitor, ... */
            .map(this::visit)
            /* ... then cast them to expressions, ...  */
            .map(x -> (Expr) x)
            /* ... and put them into a list.  */
            .collect(Collectors.toCollection(ArrayList::new));
        return new PrintStmt(getLocation(ctx), args);
    }

    @Override
    public Tree visitExpr(ExprContext ctx) {
        /* expr: additionExpr; so we just return that.  */
        return (Expr) visit(ctx.additionExpr());
    }

    @Override
    public Tree visitAdditionExpr(AdditionExprContext ctx) {
        /* Now this one is annoying.  We have a rule structure of:
             e: f (op=(OP1 | OP2 | ...) f)*;

           ... so, we have many 'f's, out of which first is initial, and the
           rest are combined in accordance to op.  To make this a little
           easier, lets label the first 'f' 'initial', and the others 'rest':

             e: initial=f (op=(OP1 | OP2 | ...) f)*;

           Following that, we can iterate 'op' and 'f' using the same indices,
           and combine them.

           See
           https://github.com/antlr/antlr4/blob/dev/doc/parser-rules.md#rule-element-labels
           ... for information about labels.  */

        var value = (Expr) visit(ctx.initial);

        assert ctx.op.size() == ctx.rest.size();
        for (int i = 0; i < ctx.op.size(); i++) {
            var op = ctx.op.get(i);
            var rhs = (Expr) visit(ctx.rest.get(i));

            var exprOp = switch (op.getType()) {
            case CalculatorLexer.PLUS -> Expr.Operation.ADD;
            case CalculatorLexer.MINUS -> Expr.Operation.SUB;
            default -> throw new IllegalArgumentException("unhandled expr op " + op);
            };

            /* For an expression A+B+C, the location spanning A+B is the
               location from the start of A to the end of B, which will
               conveniently be created by
               {@code A.getLocation().span(b.getLocation())}.  */
            var loc = value.getLocation().span(rhs.getLocation());
            value = new Expr(loc, exprOp, value, rhs);
        }
        return value;
    }

    @Override
    public Tree visitMultiplicationExpr(MultiplicationExprContext ctx) {
        /* This one is even more annoying, because it's the exact same.  It is
           possible to abstract and not specify twice, but I won't do that
           here.  It's long and ugly.  */
        var value = (Expr) visit(ctx.initial);

        assert ctx.op.size() == ctx.rest.size();
        for (int i = 0; i < ctx.op.size(); i++) {
            var op = ctx.op.get(i);
            var rhs = (Expr) visit(ctx.rest.get(i));

            /* This part changed, I guess.  */
            var exprOp = switch (op.getType()) {
            case CalculatorLexer.STAR -> Expr.Operation.MUL;
            case CalculatorLexer.SLASH -> Expr.Operation.DIV;
            default -> throw new IllegalArgumentException("unhandled expr op " + op);
            };

            var loc = value.getLocation().span(rhs.getLocation());
            value = new Expr(loc, exprOp, value, rhs);
        }
        return value;
    }

    @Override
    public Tree visitExponentExpr(ExponentExprContext ctx) {
        /* This is a right-associative operation, specified as:
             exp: term (CARET exp)?;

           ... so, it's even easier.  */
        var lhs = (Expr) visit(ctx.lhs);
        if (ctx.rhs == null)
            return lhs;

        var rhs = (Expr) visit(ctx.rhs);
        return new Expr(lhs.getLocation().span(rhs.getLocation()),
                        Expr.Operation.POW, lhs, rhs);
    }

    @Override
    public Tree visitNumberConstant(NumberConstantContext ctx) {
        /* Each labeled alternative gets its own visitor, making this quite
           convenient.  */
        return new NumberLit(getLocation(ctx), Double.parseDouble(ctx.getText()));
    }

    @Override
    public Tree visitVariableReference(VariableReferenceContext ctx) {
        /* Try to find the variable, ... */
        var loc = getLocation(ctx);
        return lookup(loc, ctx.IDENTIFIER().getText())
            /* ... and if you do find it, make it into an expression, ... */
            .map(decl -> (Tree) new VarRef(loc, decl))
            /* ... and if you fail, make it an error expression.  */
            .orElseGet(() -> new ErrorExpr(loc));
    }

    @Override
    public Tree visitGroupingOperator(GroupingOperatorContext ctx) {
        /* This one is easy.  */
        return (Expr) visit(ctx.expr());
    }

    @Override
    public Tree visitVectorConstructor(VectorConstructorContext ctx) {
        /* This one is easy, too - the rule just invokes vectorLiteral.  */
        return (Expr) visit(ctx.vectorLiteral());
    }

    @Override
    public Tree visitVectorLiteral(VectorLiteralContext ctx) {
        /* It's kinda like a function.  */
        var args = ctx.expr()
            /* Take all the parsed arguments, ... */
            .stream()
            /* ... visit them using this visitor, ... */
            .map(this::visit)
            /* ... then cast them to expressions, ...  */
            .map(x -> (Expr) x)
            /* ... and put them into a list.  */
            .collect(Collectors.toCollection(ArrayList::new));
        return new VectorExpr(getLocation(ctx), args);
    }

    /* Helpers.  */
    /** Returns the range that this subtree is in.  */
    private static Location getLocation(ParserRuleContext context) {
        return getLocation(context.getStart())
            .span(getLocation(context.getStop ()));
    }

    /** Returns the location this terminal is in.  */
    private static Location getLocation(TerminalNode term) {
        return getLocation(term.getSymbol());
    }

    /** Returns the location this token is in.  */
    private static Location getLocation(Token token) {
        /* The token starts at the position ANTLR provides us.  */
        var start = new Position(token.getLine(), token.getCharPositionInLine());

        /* But it does not provide a convenient way to get where it ends, so we
           have to calculate it based on length.  */
        assert !token.getText ().contains ("\n")
            : "CSTtoASTConverter assumes single-line tokens";
        var length = token.getText ().length ();
        assert length > 0;

        /* And then put it together.  */
        var end = new Position (start.line (), start.column () + length - 1);
        return new Location (start, end);
    }
}
