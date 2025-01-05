package rs.raf.calculator.vm;

import lombok.Data;

/** VM instruction.  See {@link VM#run(Blob)} for implementations.

    <p>Documentation of the operations reuses Forth stack effect notation.  For
    instance, if we say {@code ( l r -- l*r )}, then the opreation will take
    two stack elements {@code l} and {@code r}, such that {@code l} is under
    {@code r}, remove them from the stack, and leave their product instead of
    them.  */
@Data
public final class Instruction {

    public enum Code {
        // Arithmetic Operations
        /** {@code ( n1 n2 -- n1+n2 )}.  Addition.  */
        ADD,
        /** {@code ( n1 n2 -- n1-n2 )}.  Subtraction.  */
        SUBTRACT,
        /** {@code ( n1 n2 -- n1*n2 )}.  Multiplication.  */
        MULTIPLY,
        /** {@code ( n1 n2 -- n1/n2 )}.  Division.  */
        DIVIDE,
        /** {@code ( n1 n2 -- n1^n2 )}.  Power.  */
        RAISE,

        // Vector and List Operations
        /** {@code ( x₁ x₂ … xₙ -- <x₁,x₂,…,xₙ> )}.

            Given {@code VECTOR_CONSTRUCT n}, a vector will be constructed from
            the top {@code n} elements of the stack.  */
        VECTOR_CONSTRUCT(1),
        /** {@code ( a i -- a[i] )}

            Vector indexing.  */
        VECTOR_ACCESS,

        // Function and Return
        /** {@code ( f x₁ x₂ … xₙ -- r )}.

            Given {@code FUNCTION_CALL n}, {@code f} is an {@code n}-ary
            closure that returns {@code r} after being called on
            {@code (x₁, x₂, …, xₙ)}.  */
        FUNCTION_CALL(1),
        /** {@code ( r -- )}.

            Return value {@code r} from the function and resume execution of
            the caller.  */
        RETURN,
        /** {@code ( -- )}.

            Return no value from the function and resume execution of
            the caller.  A synthetic, unusable value will be placed on the
            callers operand stack.  */
        RETURN_VOID,
        /** {@code ( -- f )}.

            Given {@code BUILD_CLOSURE n}, {@code f} will be a closure
            populated as the function table entry {@code n} commands.  */
        BUILD_CLOSURE(1),

        // Stack Operations
        /** {@code ( -- c )}.

            Given {@code PUSH_CONSTANT n}, {@code c} will be a constant in the
            current blob constant table.  */
        PUSH_CONSTANT(1),
        /** {@code ( x -- )}. */
        POP,


        // Local variable handling.
        /** {@code ( v -- )}.

            Given {@code SET_LOCAL n}, store {@code v} in the current locals
            table in slot {@code n}.  */
        SET_LOCAL(1),
        /** {@code ( -- v )}.

            Given {@code GET_LOCAL n}, {@code v} will be the value in the
            current locals table in slot {@code n}.  */
        GET_LOCAL(1),

        // Global variable handling.
        /** {@code ( v -- )}.

            Given {@code SET_GLOBAL n}, store {@code v} in the globals table in
            slot {@code n}.  */
        SET_GLOBAL(1),
        /** {@code ( -- v )}.

            Given {@code GET_GLOBAL n}, {@code v} will be the value in the
            globals table in slot {@code n}.  */
        GET_GLOBAL(1),

        // Upvalue handling.
        /** {@code ( -- v )}.

            Given {@code GET_UPVALUE n}, {@code v} will be the value in the
            currnet upvalue table in slot {@code n}.  */
        GET_UPVALUE(1),

        // Others
        /** {@code ( x -- )}.

            Prints value {@code x} to the output stream.  */
        PRINT,
        /** {@code ( -- )}.

            Terminates executing the current blob, returning control to the
            caller of {@link VM#run(Blob)}.  */
        FINISH_OUTER,
        ;

        /** Number of arguments this instructions of this opcode take.  */
        public final int argCount;

        /** Construct an opcode which takes no values.  */
        Code() {
            this(0);
        }

        /** @param argCount Number of arguments instructions of this opcode
                            take.  Note that this is different to the number
                            of operands an instruction takes.  */
        Code(int argCount) {
            this.argCount = argCount;
        }
    }

    /** Opcode of this instruction.  */
    private final Code opcode;
    /** Argument for this instruction, if applicable.  */
    private int arg1 = -1;

    /** Construct a zero-argument instruction.

        @param opcode Opcode of this instruction.  */
    public Instruction(Code opcode) {
        assert opcode.argCount == 0;
        this.opcode = opcode;
    }

    /** Construct a one-argument instruction.

        @param opcode Opcode of this instruction.
        @param arg1 Argument to this instruction.  */
    public Instruction(Code opcode, int arg1) {
        assert opcode.argCount == 1;
        this.opcode = opcode;
        this.arg1 = arg1;
    }

    /** @param arg1 New argument to this instruction.  */
    public void setArg1(int arg1) {
        assert opcode.argCount >= 1;
        this.arg1 = arg1;
    }

    @Override
    public String toString() {
        var s = new StringBuilder();
        s.append(opcode);
        if (opcode.argCount >= 1) s.append(" ").append(arg1);
        return s.toString();
    }
}
