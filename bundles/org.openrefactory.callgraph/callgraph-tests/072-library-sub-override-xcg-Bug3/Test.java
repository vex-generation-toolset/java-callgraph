// Issue 3
// Extended call graph entry for a method that is a subclass of a library and 
// implementing a method of the library.

import org.dummy.Dummy;

public class Test {
    Dummy d = new A();
    
    public static void main(String[] args) {
        Test t = new Test();
        String m = d.b();
    }
}

class A extends Dummy {
    public String b() {
        return "ss";
    }
}

/*$$$$$ Test.main(String[]), 2,
		12, 20, 2, org.dummy.Dummy#b(), A#b(),
		11, 18, 1, Test.<init>()
*/
