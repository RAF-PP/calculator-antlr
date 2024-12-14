package rs.raf.calculator.ast;

import lombok.EqualsAndHashCode;
import lombok.Getter;


@EqualsAndHashCode
public final class Instruction {

    public enum Code {
        // Arithmetic Operations
        ADDITION,    // PLUS
        SUBTRACTION, // MINUS
        MULTIPLICATION, // STAR
        DIVISION,    // SLASH
        CARET,      // %

        // Vector and List Operations
        VECTOR_CONSTRUCT,  // Vector constructor '<'
        VECTOR_ACCESS,     // Index access in vectors

        // Function and Return
        FUNCTION_CALL,     // Calling a function
        RETURN,            // Returning from a function

        JUMP,              // Jump to another instruction, shouldn't?
        CONDITIONAL_JUMP_TRUE, // Jump if condition is true
        CONDITIONAL_JUMP_FALSE, // Jump if condition is false

        // Stack Operations
        PUSH,              // Push an element to the stack
        POP,               // Pop an element from the stack

        // Others
        PRINT,             // Print statement
        EXIT,              // Exit the program
        ;

        public final int argCount;

        Code() {
            this(0);
        }

        Code(int argCount) {
            this.argCount = argCount;
        }
    }

    private Code opcode;
    @Getter
    private long arg1 = -1;

    public Instruction(Code opcode) {
        assert opcode.argCount == 0;
        this.opcode = opcode;
    }

    public Instruction(Code opcode, long arg1) {
        assert opcode.argCount == 1;
        this.opcode = opcode;
        this.arg1 = arg1;
    }

    public Code opcode() {
        return opcode;
    }

    public void setArg1(long arg1) {
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