package rs.raf.calculator;

import rs.raf.calculator.ast.*;

public class Typecheck {
    private Calculator c;

    public Typecheck(Calculator calculator) {
        this.c = calculator;
    }

    public void typecheck(StatementList block) {
        /* Typecheck all statements.  */
        block.getStmts().forEach(this::typecheck);
    }

    private void typecheck(Statement stmt_) {
        switch (stmt_) {
        case PrintStmt stmt -> {
            /* Prints can print anything, so all are okay.  */
        }
        case Declaration stmt -> {
            /* The type of the left-hand side of a 'let' statement is the same
               as the type of the right-hand side.  This is type deduction.  */
            var newValue = typecheck(stmt.getValue());
            stmt.setValue(newValue);
            stmt.setDeclaredType(newValue.getResultType());
        }
        case ExprStmt stmt ->
            /* Just check the inner expression.  */
            stmt.setExpr(typecheck(stmt.getExpr()));

        /* Statement list logic is above.  */
        case StatementList stmt -> typecheck(stmt);
        }
    }

    /** Type-checks an expression, inserting any implicit conversions if
        needed.  Returns the same expression except annotated with types and
        with implicit conversions added.  Alters argument.  */
    private Expr typecheck(Expr expr_) {
        /* A few expressions are subclasses.  Check those separately.  */
        switch (expr_) {
        case ErrorExpr expr -> {
            /* Something went wrong.  Make up a result.  */
            expr.setResultType(c.getNumberType());
            return expr;
        }
        case NumberLit expr -> {
            /* Axiomatically a number.  */
            expr.setResultType(c.getNumberType());
            return expr;
        }
        case VarRef expr -> {
            /* Whatever the type of the variable we're looking at is.  */
            expr.setResultType(expr.getVariable().getDeclaredType());
            return expr;
        }
        case VectorExpr expr -> {
            /* We need to type check each of the arguments.  */
            for (int i = 0; i < expr.getElements().size(); i++) {
                /* Type-check and apply any conversions.  */
                var newElem = typecheck(expr.getElements().get(i));
                expr.getElements().set(i, newElem);
            }

            /* This language is entirely powered by type deduction.  To deduce
               a type of a list, we'd need the least upper bound of all the
               element types, and then to convert each element to that least
               upper bound, as that will be the element type.

               However, our type system only does numbers and lists, which do
               not have a LUB ever, nor do two distinct list types, so we can
               take the type of the first element to be our LUB.

               If it is empty, eh, make it a list of numbers.  Whatever.  */

            if (expr.getElements().isEmpty()) {
                expr.setResultType(c.listOfType(c.getNumberType()));
                return expr;
            }

            /* Element type.  */
            var eltType = expr.getElements().getFirst().getResultType();
            var vectorType = c.listOfType(eltType);
            expr.setResultType(vectorType);

            /* Now we need to try to make the elements fit the vectors.  */
            for (int i = 0; i < expr.getElements().size(); i++) {
                var newElem = tryAndConvert(eltType, expr.getElements().get(i));
                expr.getElements().set(i, newElem);
            }

            /* Done.  */
            return expr;
        }
        case Expr expr -> {
            /* Checked below.  */
        }
        }

        /* We have a regular expression here.  */
        switch (expr_.getOperation()) {
        case ADD, DIV, MUL, POW, SUB -> {
            /* Binary number expressions.  */
            expr_.setLhs(typecheck(expr_.getLhs()));
            expr_.setRhs(typecheck(expr_.getRhs()));

            /* They both must be numbers.  */
            expr_.setLhs(tryAndConvert(c.getNumberType(), expr_.getLhs()));
            expr_.setRhs(tryAndConvert(c.getNumberType(), expr_.getRhs()));

            /* The result is always a number.  */
            expr_.setResultType(c.getNumberType());
            return expr_;
        }
        case VALUE ->
            /* Shouldn't be possible.  */
            throw new IllegalStateException();
        }

        throw new IllegalStateException();
    }

    /** Attempts to make the expression EXPR fit into a place of type TO.  */
    private Expr tryAndConvert(Type to, Expr expr) {
        /* We expect the EXPR to be typechecked.  */
        assert expr.getResultType() != null;

        /* Here we could add any conversions we deem necessary.  */
        if (!expr.getResultType().equals(to)) {
            c.error(expr.getLocation(),
                    "cannot use a value of type '%s' where type '%s' is needed",
                    expr.getResultType().userReadableName(),
                    to.userReadableName());
        }

        return expr;
    }
}
