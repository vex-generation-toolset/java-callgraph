// Issue 5
// Calculation of call graphs of two methods.

public class Test {
    public static void main(String[] args) {
        Test t = new Test();
        String p = t.toString();
        System.err.println(p);
    }
}

class A {
    public M b() {
        return "ss";
    }
}

class M {
}

/*$$$$$ A#b(), 0 */
/*$$$$$ Test.main(), 0 */
