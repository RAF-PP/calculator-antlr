package rs.raf.calculator.vm;

import java.util.ArrayList;
import rs.raf.calculator.Calculator;
import static rs.raf.calculator.vm.Instruction.Code.*;

/** A VM for executing various {@link Blob}s.  */
public class VM {
    /** Wider compilation (and execution) context.  */
    private Calculator c;

    /** @param context Context to interpret in.  */
    public VM(Calculator context) {
        this.c = context;
    }

    /** Table of global values.  */
    private ArrayList<Value> globals = new ArrayList<>();

    /** Execute a single blob, concluding when it reaches {@link
        Instruction.Code#FINISH_OUTER <code>FINISH_OUTER</code>}.

        <p> Also grows the global table, as needed.

        @param blob Code to execute.  */
    public void run(Blob blob) {
        /* VM state.  */
        var ip = 0;
        var callstack = new ArrayList<BlobInvocation>();

        /* Grow the global table.  If the global count has increased, the code
           we are about to run will reference new slots.  */
        while (globals.size() < c.getGlobalCount())
            globals.add(null);

        /* Prepare the outer invocation.  */
        callstack.add(new BlobInvocation(blob));

        /* VM main loop.  */
        for (;;) {
            /* Extract information related to the currently-executing function.
               Most instructions will use these.  */
            var frame = callstack.getLast();
            var stack = frame.getOperandStack();
            var code = frame.getBlob().code();
            var csts = frame.getBlob().constantTable();
            var upvals = frame.getUpvalues();
            var locals = frame.getLocals();

            /* Fetch the current instruction.  */
            var insn = code.get(ip++);
            var op = insn.getOpcode();
            /* ... and interpret it.  */
            switch (op) {
            case ADD, DIVIDE, MULTIPLY, RAISE, SUBTRACT -> {
                /* Pop right-hand side (top of the stack).  */
                var rhs = ((Value.Number) stack.getLast()).number();
                stack.removeLast();
                /* Pop left-hand side (right under the top).  */
                var lhs = ((Value.Number) stack.getLast()).number();
                stack.removeLast();

                /* Compute the result, and push it back on the stack.  */
                stack.add(new Value.Number
                          (switch(op) {
                          case ADD -> lhs + rhs;
                          case SUBTRACT -> lhs - rhs;
                          case MULTIPLY -> lhs * rhs;
                          case DIVIDE -> lhs / rhs;
                          case RAISE -> Math.pow(lhs, rhs);
                          default ->
                              /* Impossible, save for a bug.  */
                              throw new IllegalArgumentException(op.name());
                          }));
            }

            /* Get and push a global.  */
            case GET_GLOBAL ->
                stack.add(globals.get(insn.getArg1()));
            /* Get and push a local.  */
            case GET_LOCAL ->
                stack.add(locals[insn.getArg1()]);
            /* Get and push an upvalue.  */
            case GET_UPVALUE ->
                stack.add(upvals[insn.getArg1()]);
            /* Pop and save a global.  */
            case SET_GLOBAL -> {
                globals.set(insn.getArg1(), stack.getLast());
                stack.removeLast();
            }
            /* Pop and save a local.  */
            case SET_LOCAL -> {
                locals[insn.getArg1()] = stack.getLast();
                stack.removeLast();
            }
            /* No analog for upvalues - as all values are immutable, and those
               are initialized in BUILD_CLOSURE, there's never a need to modify
               the upvalue table.  */

            /* Create a vector of values.  */
            case VECTOR_CONSTRUCT -> {
                var cnt = /* Count.  */ insn.getArg1();
                var elements = new ArrayList<>(stack.subList(stack.size() - cnt,
                                                             stack.size()));
                for (int i = 0; i < cnt; i++)
                    stack.removeLast();
                stack.add(new Value.Vector(elements));
            }

            /* Extract a vector element.  */
            case VECTOR_ACCESS -> {
                /* Unused.  */
                var rhs = ((Value.Vector) stack.getLast()).elements();
                stack.removeLast();
                var lhs = ((Value.Number) stack.getLast()).number();
                stack.removeLast();
                stack.add(rhs.get((int) lhs));
            }

            /* Get and push a constant.  */
            case PUSH_CONSTANT ->
                stack.add(new Value.Number(csts.get(insn.getArg1())));

            case POP -> stack.removeLast();


            /* Terminate this VM run (but not the program - we have a
               REPL).  */
            case FINISH_OUTER -> {
                /* This must be called from the toplevel.  */
                assert callstack.size() == 1;
                /* We must've just read the last instruction (ip is
                   post-incremented before the 'switch' we're in, so ip == size
                   in that case).  */
                assert ip == code.size();
                return;
            }
            /* Print the top of the stack.  */
            case PRINT -> {
                var val = stack.getLast();
                stack.removeLast();
                val.print(System.out);
                System.out.println();
            }

            /* Return from a function.  */
            case RETURN, RETURN_VOID -> {
                final var retVoid = op == RETURN_VOID;
                /* In case this is a void function, we don't have anything to
                   return.  But, after each ExprStmt there's a POP.  A void
                   function call is necessarily nested in a ExprStmt due to it
                   being type-checked.  With these facts, we can safely insert
                   a Java null in place of a return value, or really any
                   arbitrary value, as it will never actually be read.  */
                final var retval = retVoid ? null : stack.getLast();
                /* Restore old IP.  */
                ip = callstack.getLast().getPrevIp();
                /* Remove the last invocation.  */
                callstack.removeLast();
                /* Push the result into the now-last, previously second-last
                   invocation.  */
                callstack.getLast().getOperandStack().add(retval);
            }

            /* Call a closure.  */
            case FUNCTION_CALL -> {
                /* Arity.  */
                final var aty = insn.getArg1();
                /* Extract the closure and arguments from the stack.  */
                final var operands =
                    new ArrayList<>(stack.subList(stack.size() - aty - 1,
                                                  stack.size()));
                /* The closure is the first (deepest) thing extracted.  */
                final var closure = ((Value.Closure) operands.getFirst());

                /* Allocate a new local table for this invocation.  */
                final var newLocals = new Value[closure.localCount()];
                /* Populate it with function arguments.  */
                for (int i = 1; i < operands.size(); i++)
                    newLocals[i - 1] = operands.get(i);

                /* Construct a new invocation.  */
                final var invoc =
                    new BlobInvocation(closure.code(),
                                       /* As they will never be modified, we
                                          can reuse the same upvalue table
                                          here as we found in the closure.  */
                                       closure.upvalues(),
                                       newLocals,
                                       /* Note that the 'ip' is
                                          post-incremented before this 'switch'
                                          is executed, so 'ip' will refer to
                                          the next instruction.  */
                                       ip);
                /* Add the new invocation to the invocation stack, so that the
                   next iteration executes it.  */
                callstack.add(invoc);
                /* Reset ip to the start of the instruction.  The old one was
                   saved above.  */
                ip = 0;
                /* Pop the arguments.  */
                for (int i = 0; i < operands.size(); i++)
                    stack.removeLast();
            }

            /* Collect upvalues and build a closure!  */
            case BUILD_CLOSURE -> {
                var fn = c.getFunction(insn.getArg1());
                var um = fn.getUpvalueMap();
                /* Allocate the upvalue table.  */
                var newUpvalues = new Value[um.length];
                for (int u = 0; u < newUpvalues.length; u++)
                    /* Set upvalue u based on the specification in
                       uvalueMap[u].  */
                    newUpvalues[u] =
                        (switch (um[u].loc()) {
                            /* Fetch the correct table - if loc is UPVALUE,
                               then this is the current upvalue table,
                               otherwise it is the current locals table.  */
                        case UPVALUE -> upvals;
                        case LOCAL -> locals;
                        })[/* Extract the right slot.  */ um[u].slot()];
                /* Push the new closure onto the stack.  */
                stack.add(new Value.Closure(fn.getCode(),
                                            newUpvalues,
                                            fn.getLocalCount()));
            }
            }
        }
    }
}
