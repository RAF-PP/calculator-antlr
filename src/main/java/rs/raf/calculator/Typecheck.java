package rs.raf.calculator;

import rs.raf.calculator.ast.*;

import java.util.ArrayList;
import java.util.List;

public class Typecheck {
    private Calculator c;
    private FunctionDeclaration currentFunction;
    private List<FunctionDeclaration> functions = new ArrayList<>();

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
                stmt.getArgs().forEach(this::typecheck);
            }
            case FunctionDeclaration functionDeclaration -> {
                functionDeclaration.setDeclaredType
                    (new FunctionType(functionDeclaration.getReturnType(),
                                      functionDeclaration
                                      .getArgs()
                                      .getArguments()
                                      .stream()
                                      .map(Declaration::getDeclaredType)
                                      .toList()));

                FunctionDeclaration oldFunctionDeclaration = currentFunction;
                functions.add(functionDeclaration);
                try {
                    currentFunction = functionDeclaration;

                    if (functionDeclaration.getBody() != null) {
                        typecheck(functionDeclaration.getBody());
                    }
                } finally {
                    currentFunction = oldFunctionDeclaration;
                }
            }
            case Declaration stmt -> {
            /* The type of the left-hand side of a 'let' statement is the same
               as the type of the right-hand side.  This is type deduction.  */
                var newValue = typecheck(stmt.getValue());
                stmt.setValue(newValue);
                stmt.setDeclaredType(newValue.getResultType());
            }
            case ExprStmt stmt -> {
                /* Just check the inner expression.  */
                stmt.setExpr(typecheck(stmt.getExpr()));
            }
            case StatementList stmt -> typecheck(stmt);

            case ReturnStatement returnStatement -> {
                if (currentFunction == null) {
                    c.error(returnStatement.getLocation(),
                            "return outside function");
                    break;
                }
                var cfn = currentFunction.getName();
                var rt = (Type) currentFunction.getReturnType();
                var needsReturn = !(rt instanceof VoidType);
                var hasReturn = returnStatement.getValue() != null;

                if (needsReturn && !hasReturn)
                    c.error(currentFunction.getLocation(),
                            "function '%s' needs a return value, but none was given",
                            cfn);
                else if (!needsReturn && hasReturn)
                    c.error(returnStatement.getValue().getLocation(),
                            "function '%s' does not return a value, but one was given",
                            cfn);

                if (hasReturn && needsReturn)
                    returnStatement.setValue(typecheck(returnStatement.getValue()));
            }
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
                var eltType = expr.getElements().get(0).getResultType();
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

            case FunctionCall expr -> {
                // Get the function name and arguments
                var callee = typecheck(expr.getFunction());
                List<Type> argumentTypes = new ArrayList<>();

                // Collect the argument types from the expression
                for (Expr arg : expr.getArguments()) {
                    typecheck(arg);
                    argumentTypes.add(arg.getResultType()); // Assuming each argument is already typechecked
                }

                // Look up the function in the symbol table or function registry
                var calleeType = callee.getResultType();

                if (!(calleeType instanceof FunctionType function)) {
                    // Function not found, report an error
                    c.error(expr.getLocation(),
                            "Trying to call non-function of type '%s'.",
                            calleeType.userReadableName()
                    );
                    return expr;
                }
                // Function found, set the result type to the return type of the function
                expr.setResultType(function.getReturnType());

                final var expectedArgCount = function.getArgumentTypes().size();
                final var argCount = expr.getArguments().size();
                if (argCount != expectedArgCount) {
                    c.error(expr.getLocation(),
                            "Trying to call function '%s' with %d arguments, but expected %d.",
                            callee, argCount, expectedArgCount);
                    return expr;
                }

                // Type-check and convert each argument against the corresponding parameter type
                for (int i = 0; i < argCount; i++) {
                    Type paramType = function.getArgumentTypes().get(i); // Expected type for this argument
                    Expr arg = expr.getArguments().get(i); // Actual argument in the call

                    // Try to convert the argument to the expected type
                    tryAndConvert(paramType, arg); // This will throw an error if conversion fails
                }
                return expr;
            }
            default -> {
                // Handle other expressions that aren't explicitly handled
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
            case VALUE -> {
                /* Shouldn't be possible.  */
                throw new IllegalStateException();
            }
        }

        throw new IllegalStateException();
    }

    private Expr tryAndConvert(Type expectedType, Expr expr) {
        // Try to convert the expression to the expected type
        if (expr.getResultType().equals(expectedType)) {
            return expr;
        } else {
            // If conversion isn't possible, report an error
            c.error(expr.getLocation(),
                    "Cannot convert expression of type '%s' to expected type '%s'.",
                    expr.getResultType().userReadableName(), expectedType.userReadableName());
            return expr;
        }
    }
}
