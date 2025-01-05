package rs.raf.calculator.vm;

import java.util.ArrayList;
import rs.raf.calculator.Calculator;
import static rs.raf.calculator.vm.Instruction.Code.*;

public class VM {
    private Calculator c;

    public VM(Calculator context) {
        this.c = context;
    }

    private ArrayList<Value> globals = new ArrayList<>();

    public void run(Blob blob) {
        int ip = 0;
        var callstack = new ArrayList<BlobInvocation>();

        while (globals.size() < c.getGlobalCount())
            globals.add(null);

        /* Prepare the outer invocation.  */
        callstack.add(new BlobInvocation(blob));

        for (;;) {
            var frame = callstack.getLast();
            var stack = frame.getOperandStack();
            var code = frame.getBlob().code();
            var csts = frame.getBlob().constantTable();
            var upvals = frame.getUpvalues();
            var locals = frame.getLocals();

            var insn = code.get(ip++);
            var op = insn.getOpcode();
            switch (op) {
            case ADD, DIVIDE, MULTIPLY, RAISE, SUBTRACT -> {
                var rhs = ((Value.Number) stack.getLast()).number();
                stack.removeLast();
                var lhs = ((Value.Number) stack.getLast()).number();
                stack.removeLast();
                stack.add(new Value.Number
                          (switch(op) {
                          case ADD -> lhs + rhs;
                          case SUBTRACT -> lhs - rhs;
                          case MULTIPLY -> lhs * rhs;
                          case DIVIDE -> lhs / rhs;
                          case RAISE -> Math.pow(lhs, rhs);
                          default ->
                              throw new IllegalArgumentException(op.name());
                          }));
            }

            case GET_GLOBAL ->
                stack.add(globals.get(insn.getArg1()));
            case GET_LOCAL ->
                stack.add(locals[insn.getArg1()]);
            case GET_UPVALUE ->
                stack.add(upvals[insn.getArg1()]);
            case SET_GLOBAL -> {
                globals.set(insn.getArg1(), stack.getLast());
                stack.removeLast();
            }
            case SET_LOCAL -> {
                locals[insn.getArg1()] = stack.getLast();
                stack.removeLast();
            }

            case VECTOR_CONSTRUCT -> {
                var cnt = /* Count.  */ insn.getArg1();
                var elements = new ArrayList<>(stack.subList(stack.size() - cnt,
                                                             stack.size()));
                for (int i = 0; i < cnt; i++)
                    stack.removeLast();
                stack.add(new Value.Vector(elements));
            }
            case VECTOR_ACCESS -> {
                /* Unused.  */
                var rhs = ((Value.Vector) stack.getLast()).elements();
                stack.removeLast();
                var lhs = ((Value.Number) stack.getLast()).number();
                stack.removeLast();
                stack.add(rhs.get((int) lhs));
            }

            case PUSH_CONSTANT ->
                stack.add(new Value.Number(csts.get(insn.getArg1())));
            case POP -> stack.removeLast();

            case FINISH_OUTER -> {
                assert callstack.size() == 1;
                assert ip == code.size();
                return;
            }
            case PRINT -> {
                var val = stack.getLast();
                stack.removeLast();
                val.print(System.out);
                System.out.println();
            }

            case RETURN, RETURN_VOID -> {
                final var retVoid = op == RETURN_VOID;
                /* In case this is a void function, we don't have anything to
                   return.  But, after each ExprStmt there's a POP.  A void
                   function call is necessarily nested in a ExprStmt due to it
                   being type-checked.  With these facts, we can safely insert
                   a Java null in place of a return value, or really any
                   arbitrary value, as it will never actually be read.  */
                final var retval = retVoid ? null : stack.getLast();
                ip = callstack.getLast().getPrevIp();
                callstack.removeLast();
                callstack.getLast().getOperandStack().add(retval);
            }
            case FUNCTION_CALL -> {
                final var aty = insn.getArg1();
                final var operands =
                    new ArrayList<>(stack.subList(stack.size() - aty - 1,
                                                  stack.size()));
                final var closure = ((Value.Closure) operands.getFirst());

                final var newLocals = new Value[closure.localCount()];
                for (int i = 1; i < operands.size(); i++)
                    newLocals[i - 1] = operands.get(i);

                final var invoc =
                    new BlobInvocation(closure.code(),
                                       closure.upvalues(),
                                       newLocals,
                                       ip);
                callstack.add(invoc);
                ip = 0;
                for (int i = 0; i < operands.size(); i++)
                    stack.removeLast();
            }
            case BUILD_CLOSURE -> {
                var fn = c.getFunction(insn.getArg1());
                var um = fn.getUpvalueMap();
                var newUpvalues = new Value[um.length];
                for (int u = 0; u < newUpvalues.length; u++)
                    newUpvalues[u] =
                        (switch (um[u].loc()) {
                        case UPVALUE -> upvals;
                        case LOCAL -> locals;
                        })[um[u].slot()];
                stack.add(new Value.Closure(fn.getCode(),
                                            newUpvalues,
                                            fn.getLocalCount()));
            }
            }
        }
    }
}
