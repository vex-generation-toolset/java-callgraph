// Issue 3
// Extended call graph entry for inner class inside static nested class.

package org.openrefactory.test;

class Outer {
    static class Nested {
        class InnerInNested {
            void show() {
                System.out.println("Inner inside Static Nested");
            }
        }
    }

    public static void main(String[] args) {
        Outer.Nested nested = new Outer.Nested();
        Outer.Nested.InnerInNested inner = nested.new InnerInNested();
        inner.show();
    }
}


/*$$$$$ org.openrefactory.test.Outer.main(String[]), 
   3,
   org.openrefactory.test.Outer.Nested.<init>(),
   org.openrefactory.test.Outer.Nested.InnerInNested.<init>(),
   org.openrefactory.test.Outer.Nested.InnerInNested#show(),
 */

/*!!!!! org.openrefactory.test.Outer.main(String[]), 3, 386,18, 449,26, 485,12 */
