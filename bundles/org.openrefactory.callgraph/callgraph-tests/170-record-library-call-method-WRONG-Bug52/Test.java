// Issue 52
// Test for record with method that calls a library method

record Name(String val) {
    public static void printUpper() {
        String upper = "hello".toUpperCase();
    }
}

public class Demo {
    public static void foo() {
        Name n = new Name("hello");
        n.printUpper();
    }
}

/*$$ Demo.foo(),
  2,
  Name#Name(String),
  Name.printUpper()
*/

/*$$ Name.printUpper(),
  1,
  java.lang.String#toUpperCase()
*/

// This test is currently disabled because it fails in extended callgraph test suite run.
// NB: it runs correctly inside projects.
