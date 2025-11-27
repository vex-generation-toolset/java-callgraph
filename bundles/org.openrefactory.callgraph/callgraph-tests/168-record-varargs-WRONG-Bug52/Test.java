// Issue 52

record R(int... vals) {}

public class Demo {
    public static void foo() {
        R r = new R(1, 2, 3);
    }
}

/*$$ Demo.foo(),
  1,
  R#R(int[]...)
*/

// This test is currently disabled because it fails in extended callgraph test suite run.
// NB: it runs correctly inside projects.
