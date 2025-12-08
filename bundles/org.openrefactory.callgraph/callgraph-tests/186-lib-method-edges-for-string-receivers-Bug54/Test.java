// Issue 54
// Inheritance scenario with String substring on a value from the superclass

class BaseName {
    protected String baseLabel() {
        return "BASE-NAME";
    }
}

class ChildName extends BaseName {
    public void process() {
        String suffix = baseLabel().substring(5);
    }
}

public class Demo {
    public static void fooInheritanceComplex() {
        new ChildName().process();
    }
}

/*$$$$$ Demo.fooInheritanceComplex(), 1,
  		18, 9, 2, ChildName.<init>(), ChildName#process()
*/

/*$$$$$ ChildName#process(), 1,
  		12, 25, 2, BaseName#baseLabel(), java.lang.String#substring(int)
*/
