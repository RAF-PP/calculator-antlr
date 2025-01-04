package rs.raf.calculator.compiler;

import java.util.IdentityHashMap;
import rs.raf.calculator.Calculator;
import rs.raf.calculator.ast.*;
import rs.raf.calculator.compiler.InTranslationBlob.UpvalSlotInfo;
import rs.raf.calculator.vm.*;

/* So that instructions can be used unqualified.  */
import static rs.raf.calculator.vm.Instruction.Code.*;

public class Compiler {
    /* This class should not emit errors.  */
    private final Calculator c;

    public Compiler(Calculator calculator) {
        this.c = calculator;
    }

    /** A "sphagetti-stack" of blobs of code.  Each blob of code, except for
        the outermost one, is related to a function, and hence has a table of
        local values, as well as a table of "upvalues", which are variables
        copied from the outer scope.  */
    private InTranslationBlob blob = null;

    private int emit(Instruction.Code opcode) {
        return emit(new Instruction(opcode));
    }

    private int emit(Instruction.Code opcode, int arg1) {
        return emit(new Instruction(opcode, arg1));
    }

    private int emit(Instruction insn) {
        return blob.getCode().addInsn(insn);
    }

    /** Compiles a single global scope statement list and produces a blob of
        code for it that the VM can interpret immediately.  Populates the
        function table if a function is declared within {@code input}.  */
    public Blob compileInput(StatementList input) {
        assert !(c.hadError() || c.hadRuntimeError());
        /* This function should only be called for the global scope.  */
        assert blob == null;
        var outerBlob = new InTranslationBlob(new Blob(),
                                              null,
                                              null,
                                              blob);
        /* Push.  */
        blob = outerBlob;

        compileBlock(input);
        /* Used as a signal to our VM that we're done with the blob.  */
        emit(FINISH_OUTER);

        /* We must've come back down to the bottom of the stack.  */
        assert blob == outerBlob;
        blob = null;

        /* There can't possibly be any locals here.  */
        assert outerBlob.getMaxLocalDepth() == 0;
        return outerBlob.getCode();
    }

    private void compileBlock(StatementList input) {
        var currentBlob = blob;
        var oldLocalDepth = currentBlob.getLocalDepth();

        for (var statement : input.getStmts())
            compileStatement(statement);

        blob.setLocalDepth(oldLocalDepth);
        assert currentBlob == blob;
    }

    private Instruction declareVariable(Declaration declaration) {
        if (blob.getPreviousBlob() == null) {
            /* New global variable.  */
            return new Instruction(SET_GLOBAL, c.declareGlobal(declaration));
        } else {
            var newVarId = blob.getLocalDepth();
            blob.setLocalDepth(newVarId + 1);
            var oldId = blob.getLocalSlots().put(declaration, newVarId);
            assert oldId == null : "how did you redeclare it??";
            return new Instruction(SET_LOCAL, newVarId);
        }
    }

    private int compileFunction(FunctionDeclaration fn) {
        var function = new Function();
        function.setFuncDecl(fn);
        var functionBlob = new InTranslationBlob(new Blob(),
                                                 new IdentityHashMap<>(),
                                                 new IdentityHashMap<>(),
                                                 blob);
        blob = functionBlob;
        var newFnId = c.addFunction(function);

        /* Declare variables.  */
        fn.getArgs().getArguments().forEach(this::declareVariable);

        /* Compile body.  */
        compileBlock(fn.getBody());

        /* Populate the function data.  */
        function.setCode(functionBlob.getCode());
        var upvals = new UpvalueMapEntry[functionBlob.getUpvalSlots().size()];
        function.setUpvalueMap(upvals);
        function.setLocalCount(functionBlob.getMaxLocalDepth());
        functionBlob.getUpvalSlots()
            .values()
            .forEach(s -> { upvals[s.slotNr()] = s.entry(); });

        /* Pop.  */
        blob = blob.getPreviousBlob();
        return newFnId;
    }

    private Instruction findLocalInsn(InTranslationBlob blob,
                                      Declaration decl) {
        /* We already checked globals in getVarInsn.  */
        var locals = blob.getLocalSlots();
        var upvals = blob.getUpvalSlots();
        assert locals != null && upvals != null;

        var local = locals.get(decl);
        if (local != null)
            return new Instruction(GET_LOCAL, local);

        /* So, this is a upvalue.  But is it new?  */
        var upval = blob.getUpvalSlots().get(decl);
        if (upval != null)
            /* No, it isn't.  */
            return new Instruction(GET_UPVALUE, upval.slotNr());

        /* It is.  */
        var inSuperscope = findLocalInsn(blob.getPreviousBlob(), decl);
        var upvalSlot = blob.getUpvalSlots().size();

        var upvalME = new UpvalueMapEntry(switch (inSuperscope.getOpcode()) {
            case GET_LOCAL -> UpvalueMapEntry.UpvalueLocation.LOCAL;
            case GET_UPVALUE -> UpvalueMapEntry.UpvalueLocation.UPVALUE;
            default -> throw new IllegalArgumentException();
            }, Math.toIntExact(inSuperscope.getArg1()));

        var oldSlot = blob.getUpvalSlots()
            .put(decl, new UpvalSlotInfo(upvalSlot, upvalME));
        assert oldSlot == null;
        return new Instruction(GET_UPVALUE, upvalSlot);
    }

    private Instruction getVarInsn(Declaration decl) {
        return c.getGlobalSlot(decl)
            .map(s -> new Instruction(GET_GLOBAL, s))
            .orElseGet(() -> findLocalInsn(blob, decl));
    }

    private void compileStatement(Statement stmt) {
        switch (stmt) {
        case ExprStmt es -> {
            compileExpr(es.getExpr());
            emit(POP);
        }

        case PrintStmt print -> {
            /* The VM contains a {@code PRINT} instruction that prints a single
               operand from the operand stack, however our language contains a
               n-ary {@code print} statement.  We must, then, implement the
               {@code print} statement as many {@code PRINT}s.  */
            print.getArgs().forEach
                (expr -> {
                    compileExpr(expr);
                    emit(PRINT);
                });
        }

        case ReturnStatement ret -> {
            if (ret.getValue() != null) {
                compileExpr(ret.getValue());
                emit(RETURN);
            } else
                emit(RETURN_VOID);
        }

        case FunctionDeclaration fn -> {
            var newVarSetter = declareVariable(fn);
            var fnId = compileFunction(fn);
            emit(BUILD_CLOSURE, fnId);
            emit(newVarSetter);
        }

        case Declaration decl -> {
            var newVarSetter = declareVariable(decl);
            compileExpr(decl.getValue());
            emit(newVarSetter);
        }

        case StatementList block ->
            compileBlock(block);
        }
    }

    private void compileExpr(Expr expr) {
        switch (expr) {
        case ErrorExpr ignored -> throw new IllegalStateException();
        case FunctionCall call -> {
            compileExpr(call.getFunction());
            call.getArguments().forEach(this::compileExpr);
            emit(FUNCTION_CALL, call.getArguments().size());
        }
        case VarRef var ->
            emit(getVarInsn(var.getVariable()));
        case VectorExpr vector -> {
            vector.getElements().forEach(this::compileExpr);
            emit(VECTOR_CONSTRUCT, vector.getElements().size());
        }
        case NumberLit numlit -> {
            var constantNumber = blob.getCode().constantTable().size();
            blob.getCode().constantTable().add(numlit.getValue());
            emit(PUSH_CONSTANT, constantNumber);
        }
        case Expr binaryExpr -> {
            /* Must not be a subclass.  */
            assert binaryExpr.getClass() == Expr.class;
            compileExpr(binaryExpr.getLhs());
            compileExpr(binaryExpr.getRhs());
            emit(switch (binaryExpr.getOperation()) {
                case ADD -> ADD;
                case DIV -> DIVIDE;
                case MUL -> MULTIPLY;
                case SUB -> SUBTRACT;
                case POW -> RAISE;
                default -> throw new IllegalArgumentException();
                });
        }
        }
    }
}
