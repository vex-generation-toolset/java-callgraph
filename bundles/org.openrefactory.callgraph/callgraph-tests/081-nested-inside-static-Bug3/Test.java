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

/*$$$$$ org.openrefactory.test.Outer.main(String[]), 3,
		16,31, 1, org.openrefactory.test.Outer.Nested.<init>(),
		17,44, 1, org.openrefactory.test.Outer.Nested.InnerInNested.<init>(),
		18,9, 1, org.openrefactory.test.Outer.Nested.InnerInNested#show(),
*/
