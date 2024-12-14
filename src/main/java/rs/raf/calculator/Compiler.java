package rs.raf.calculator;

import lombok.Getter;
import rs.raf.calculator.ast.*;

import java.beans.Expression;
import java.util.ArrayList;
import java.util.IdentityHashMap;

public class Compiler {

    public static void generateCalculatorIR (Calculator calculator) {
        var compilter = new Compiler(calculator);
//        c.compiler (calculator.getAstNode())
    }

    @Getter
    private final ArrayList<Instruction> instructions = new ArrayList<> ();

    private final Calculator c;
    private int localCount = 0;
    private Object LOCAL_INTEGER_KEY = new Object ();
//    private final IdentityHashMap<FuncDecl, Integer> functionLUT;

    public Compiler ( Calculator calculator) {
        this.c = calculator;
    }

    private void compile (StatementList statementList) {
        var parentLocals = localCount;
        for (var stmt : statementList.getStmts ())
            compileStmt (stmt);

        for (; localCount != parentLocals; localCount--)
            emit (Instruction.Code.POP);
    }

    private void compileStmt (Statement stmt) {
        switch (stmt) {
            case Declaration declaration -> {

            }
            case ExprStmt exprStmt -> {

            }
            case PrintStmt printStmt -> {

            }
            case ReturnStatement returnStmt -> {

            }
            default -> throw new IllegalStateException("Unexpected value: " + stmt);
        }
    }

    private record IPInsn (int ip, Instruction insn) {}

    private IPInsn
    emit (Instruction.Code code)
    {
        var insn = new Instruction (code);
        var ip = instructions.size ();
        instructions.add (insn);
        return new IPInsn (ip, insn);
    }

}
