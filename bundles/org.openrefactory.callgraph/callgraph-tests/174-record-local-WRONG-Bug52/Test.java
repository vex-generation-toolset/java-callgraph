// Issue 52
// Test for record local variables with generics

import java.util.List;

public class Demo {
    public void foo() {
        record Inner(List<String> list) {}
        Inner inner = new Inner(List.of("hello"));
        inner.list().get(0).toUpperCase();
    }
}

/*$$ Demo#foo(),
  5,
  Demo#foo().Inner#list(),
  Inner#Inner(Object),
  java.lang.String#toUpperCase(),
  java.util.List#get(int),
  java.util.List.of(String)
*/

// This test is currently disabled because it fails in extended callgraph test suite run.
// NB: it runs correctly inside projects.
