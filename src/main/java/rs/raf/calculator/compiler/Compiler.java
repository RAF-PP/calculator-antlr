package rs.raf.calculator.compiler;

import java.util.IdentityHashMap;

import lombok.RequiredArgsConstructor;
import rs.raf.calculator.Calculator;
import rs.raf.calculator.ast.*;
import rs.raf.calculator.compiler.InTranslationBlob.UpvalSlotInfo;
import rs.raf.calculator.vm.*;

/* So that instructions can be used unqualified.  */
import static rs.raf.calculator.vm.Instruction.Code.*;

/** Turns ASTs into {@link Blob "blobs"}, populating the {@link Calculator}
    function table as it does so.

    <p> The compiler is given an AST node representing the code in the global
    scope, compiles it into a blob and returns that blob.  The blob it returns
    is special in that it does not represent a function body, and, ergo, lacks
    locals and upvalues.  It is called the toplevel or global blob.

    <p> While translating code, in-progress blobs are organized in a stack - as
    such, each blob has a preceding (or parent) blob.  The parent of blob
    {@code B} is the blob that <i>was</i> being translated before we had to
    start translating {@code B} instead.

    <p> While translating this AST node, the compiler might encounter function
    definitions.  When it does so, it creates a new {@link InTranslationBlob
    context} for the function it's about to start translating, and saves the
    function into the function table.  After it finishes compiling the
    function, it generates a {@link Instruction.Code#BUILD_CLOSURE
    <code>BUILD_CLOSURE</code>} instruction that will, when executed by the VM,
    read the function table and generate a {@link Value.Closure}.  It is then
    saved as a global or local depending on context using {@link
    Instruction.Code#SET_GLOBAL <code>SET_GLOBAL</code>} or {@link
    Instruction.Code#SET_LOCAL <code>SET_LOCAL</code>}.

    <p> Variables in the toplevel blob are global.  Their values are accessed
    using {@link Instruction.Code#GET_GLOBAL <code>GET_GLOBAL</code>} and
    modified using {@link Instruction.Code#SET_GLOBAL <code>SET_GLOBAL</code>}.
    Function slots are allocated using {@link
    Calculator#declareGlobal(Declaration)}.

    <p> While translating a function, if a declaration is translated that is
    neither in the blobs table of local values nor in the table of global
    values, it is considered an "upvalue".  Upvalues are values stored in the
    closure, populated at time of closure creation.  As upvalues may be in an
    outer or parent blob, searching for them works like so:

    <ol>

      <li>First, check the local table of this function.  If this declaration
      is local, return a reference to it.</li>

      <li>Otherwise, it is an upvalue of some sort.  Check the upvalue table of
      the current function.  If this declaration is present in it, return a
      reference to it.</li>

      <li>Otherwise, this is a new upvalue.  Recurse into the parent blob.
      This recursion will return a reference to a local variable or an upvalue
      in the parent.</li>

      <li>Allocate a new upvalue slot for this new upvalue.  Set its mapping to
      the reference returned in the previous step.</li>

      <li>Return a reference to this new upvalue slot.</li>

    </ol>

    This algorithm is implemented in {@link #findLocalInsn(InTranslationBlob,
    Declaration)}.  It is called <code>findLocal<b>Insn</b></code> because it
    actually returns one of {@code GET_LOCAL n} or {@code GET_UPVALUE n}, where
    {@code n} is in the slot in the appropriate table, and the opcode is {@code
    GET_LOCAL} if the reference is to a local, otherwise {@code GET_UPVALUE}.
*/
@RequiredArgsConstructor
public class Compiler {
    /* This class should not emit errors.  */
    /** Wider compilation context.  */
    private final Calculator c;

    /** A "sphagetti-stack" of blobs of code.  Each blob of code, except for
        the outermost one, is related to a function, and hence has a table of
        local values, as well as a table of "upvalues", which are variables
        copied from the outer scope.  */
    private InTranslationBlob blob = null;

    /** Emit a zero argument instruction with opcode {@code opcode} into the
        current top in-translation blob.
        @param opcode Opcode of the new instruction.
        @return The IP of the new instruction.  */
    private int emit(Instruction.Code opcode) {
        return emit(new Instruction(opcode));
    }

    /** Emit a single argument instruction with opcode {@code opcode} into the
        current top in-translation blob.
        @param opcode Opcode of the new instruction.
        @param arg1 Argument of the new instruction.
        @return The IP of the new instruction.  */
    private int emit(Instruction.Code opcode, int arg1) {
        return emit(new Instruction(opcode, arg1));
    }

    /** Emit an arbitrary instruction {@code insn} into the current top
        in-translation blob.
        @param insn The new instruction.
        @return The IP of the new instruction.  */
    private int emit(Instruction insn) {
        return blob.getCode().addInsn(insn);
    }

    /** Compiles a single global scope statement list and produces a blob of
        code for it that the VM can interpret immediately.  Populates the
        function table if a function is declared within {@code input}.

        @param input Root of the newly-loaded AST.

        @return A blob that should be executed immediately, representing the
                code in the global scope of the program.  */
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

    /** Compile a block AST node.  Blocks introduce a new scope.  For two
        sibling scopes (i.e. scopes that have the same direct parent),
        variables in the first scope are necessarily not available in the
        second scope.  Variables in each scope are also not available in the
        parent.  As a result, we can overwrite variables we used in a scope
        after we exit from the scope.

        <p> This function translates a block, emitting its code, and also
        restores the local variable depth after its done.

        @param input AST node to translate.  */
    private void compileBlock(StatementList input) {
        var currentBlob = blob;
        var oldLocalDepth = currentBlob.getLocalDepth();

        for (var statement : input.getStmts())
            compileStatement(statement);

        blob.setLocalDepth(oldLocalDepth);
        assert currentBlob == blob;
    }

    /** Allocate a slot for a variable, either a local or a global one, and
        return an instruction that sets the top of the stack into that slot.

        @param declaration Declaration to assign a slot to

        @return An instruction that pops the top of the stack into the
        newly-allocated slot.  */
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

    /** Temporarily suspend compiling the current function in order to compile
        function {@code fn}.

        This algorithm determines the necessary upvalues for the new function,
        the needed number of locals, and saves that information into the
        function table.

        @param fn Function declaration to compile.

        @return The function table slot given to this function.  */
    private int compileFunction(FunctionDeclaration fn) {
        var function = new Function();
        function.setFuncDecl(fn);

        /* Push a new in-translation blob, suspending the translation of the
           previous one.  */
        var functionBlob = new InTranslationBlob(new Blob(),
                                                 new IdentityHashMap<>(),
                                                 new IdentityHashMap<>(),
                                                 blob);
        blob = functionBlob;
        var newFnId = c.addFunction(function);

        /* Declare function arguments into the first few slots.  We do this
           because the VM will, in response to CALL, place the arguments it
           reads off of the argument stack into the first local slots.  */
        fn.getArgs().getArguments().forEach(this::declareVariable);

        /* Compile body as a usual block.  We've set up the compiler state so
           that the code the compiler emits while translating this function
           ends up in the new function.  */
        compileBlock(fn.getBody());

        /* Populate the function data.  */
        function.setCode(functionBlob.getCode());
        function.setLocalCount(functionBlob.getMaxLocalDepth());

        /* Set up the upvalue map.  */
        var upvals = new UpvalueMapEntry[functionBlob.getUpvalSlots().size()];
        function.setUpvalueMap(upvals);
        functionBlob.getUpvalSlots()
            .values()
            /* The function blob contains a mapping from declarations to the
               slots they are in.  This time, we only care about the slots.
               For each of those slots. set it up in the new upvalue map using
               the upvalue reference we computed earlier.  */
            .forEach(s -> { upvals[s.slotNr()] = s.entry(); });

        /* Pop.  */
        blob = blob.getPreviousBlob();
        return newFnId;
    }

    /** Generate the instruction for accessing the local variable or upvalue
        {@code decl}.  Refer to the class implementation for details of the
        algorithm.

        <p><b>Note:</b> users that want to get the code for variable access
        should instead use {@link #getVarInsn(Declaration)}, as it also checks
        the global table.

        @param blob The blob to search for this instruction in.
        @param decl The decl to find.

        @return An instruction that accesses {@code decl} if placed in {@code
                blob}.  */
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

    /** Generate the instruction for access to the variable {@code decl} in the
        current {@link #blob}.

        @param decl Variable to access

        @return A new instruction that will push the value of the variable
                {@code decl} onto the top of the operand stack.  */
    private Instruction getVarInsn(Declaration decl) {
        return c.getGlobalSlot(decl)
            .map(s -> new Instruction(GET_GLOBAL, s))
            .orElseGet(() -> findLocalInsn(blob, decl));
    }

    /** Emit the code for {@code stmt} in the current {@link #blob}.

        @param stmt Statement to translate.  */
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

    /** Emit code to compute the expression {@code expr} and leave its result
        as the top operand of the operand stack.  If the stack depth before
        executing the code emitted by this function is {@code n}, then, after
        executing the code emitted by this function, the stack depth will be
        {@code n + 1}, i.e. only a single extra value—the result of the
        expression—will be on the stack, and it will be on the top.

        <p><b>Note:</b> Calls of {@code void} functions <i>will</i> generate an
        extra operand on the operand stack, but that operand <i>will not</i> be
        usable in any operation.

        @param expr Expression AST to translate.  */
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
