// Issue 54
// Test for library instance method edges in an inheritance hierarchy

class ParentName {
    protected String provideName() {
        return "ParentUser";
    }
}

class ChildName extends ParentName {
    public void formatInherited() {
        String lower = provideName().toLowerCase();
    }
}

public class Demo {
    public static void fooInherited() {
        new ChildName().formatInherited();
    }
}

/*$$$$$ Demo.fooInherited(), 1,
  		18, 9, 2, ChildName.<init>(), ChildName#formatInherited()
*/

/*$$$$$ ChildName#formatInherited(), 1,
  		12, 24, 2, ParentName#provideName(), java.lang.String#toLowerCase()
*/
