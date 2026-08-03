// Issue 69
// The shape of a repackaged ASM call, mw.visitVarInsn(BIPUSH, ch), where the second actual
// is a char and the formal is an int. The opcode is a local rather than a static field so
// the call site does not also pick up a static initializer edge.

class MethodWriter {
    void visitVarInsn(int opcode, int var) {
    }
}

public class Z {
    void foo(MethodWriter mw, char ch) {
        int opcode = 16;
        mw.visitVarInsn(opcode, ch);
    }
}

/*$$$$$ Z#foo(MethodWriter@@@char), 1,
        14,9, 1, MethodWriter#visitVarInsn(int@@@int)
*/
