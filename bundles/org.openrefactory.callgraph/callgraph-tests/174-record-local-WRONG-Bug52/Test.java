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

/*$$ Demo#foo(), 3,
  		9,23, 1, Inner#Inner(Object),
  		9,33, 1, java.util.List.of(String),
  		10,9, 1, Demo#foo().Inner#list(), java.util.List#get(int), java.lang.String#toUpperCase(),
*/

// This test is currently disabled because it fails in extended callgraph test suite run.
// NB: it runs correctly inside projects.
