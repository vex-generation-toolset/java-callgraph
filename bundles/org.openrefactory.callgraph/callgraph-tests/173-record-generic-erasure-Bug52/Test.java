// Issue 52
// Test for local record generic erasure.


import java.util.List;

public class ErasureDemo {
    // Compile-time signature: foo(List<String>)
    // Runtime signature:      foo(List)
    public void foo(List<String> list) {
        String s = "String".toUpperCase();
    }
}

/*$$$$$ ErasureDemo#foo(List), 1,
  		11,20, 1, java.lang.String#toUpperCase()
*/
